package io.vacco.a4lb.impl;

import com.google.gson.Gson;
import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.niossl.SSLCertificates;
import io.vacco.a4lb.util.*;
import org.slf4j.*;

import java.net.http.HttpClient;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

import static io.vacco.a4lb.util.A4Logging.onError;
import static io.vacco.a4lb.util.A4Valid.validationsOf;
import static io.vacco.a4lb.impl.A4DiscoverCore.*;
import static io.vacco.a4lb.impl.A4DiscoverK8s.k8sDiscover;

public class A4Discover implements Callable<List<A4Backend>> {

  public static Integer MaxDiscoveryErrorLogs = 1024;
  public static int LogIntervalMultiplier = 10;

  private static final Logger log = LoggerFactory.getLogger(A4Discover.class);

  private final String serverId;
  private final A4Match match;
  private final Gson gson;
  private final A4TtlMap<Integer, String> errorLogIdx = new A4TtlMap<>(MaxDiscoveryErrorLogs, msg -> {});
  private final HttpClient client = HttpClient
    .newBuilder()
    .sslContext(SSLCertificates.trustAllContext())
    .build();

  public A4Discover(String serverId, A4Match match, Gson gson) {
    this.serverId = Objects.requireNonNull(serverId);
    this.match = Objects.requireNonNull(match);
    this.gson = Objects.requireNonNull(gson);
    Objects.requireNonNull(match.discover);
  }

  private List<A4Backend> discover() {
    if (match.discover.exec != null) {
      return execDiscover(match, gson);
    } else if (match.discover.http != null) {
      return httpDiscover(match, gson);
    } else if (match.discover.k8s != null) {
      return k8sDiscover(match, client);
    }
    log.error("{} - unsupported discovery method", serverId);
    return Collections.emptyList();
  }

  private List<A4Backend> discoverValid() {
    var bkl = new ArrayList<A4Backend>();
    for (var bk : discover()) {
      if (match.pool.type == A4PoolType.weight) {
        if (bk.weight == null || bk.priority == null) {
          bk.weight(1).priority(1);
          onError(log, "{} - backend entry missing weight/priority, assigned defaults - {}", null, serverId, bk);
        }
      }
      var errors = A4Valid.validate(bk);
      if (errors.isEmpty()) {
        bkl.add(bk);
      } else {
        onError(log, "{} - invalid backend definition {} {}", null, serverId, bk, validationsOf(errors));
      }
    }
    return bkl;
  }

  private int hashOf(List<A4Backend> backends) {
    if (backends == null || backends.isEmpty()) {
      return 0;
    }
    var hlCodes = backends.stream()
      .map(h -> String.format("%x", h.addr.hashCode())).sorted()
      .collect(Collectors.joining(":"));
    return hlCodes.hashCode();
  }

  @Override public List<A4Backend> call() {
    try {
      var bkl = discoverValid();
      int h0 = hashOf(bkl);
      int h1 = hashOf(match.pool.hosts);
      if (h0 != h1) {
        log.info("{} - backend list update for match [{}] - {}", serverId, match, bkl);
        return bkl;
      }
    } catch (Exception e) {
      var msg = String.format("%s - backend discovery error for match [%s]", serverId, match);
      if (errorLogIdx.get(msg.hashCode()) == null) {
        onError(log, msg, e);
        errorLogIdx.put(msg.hashCode(), msg, match.discover.intervalMs * LogIntervalMultiplier);
      }
    }
    return null;
  }

}
