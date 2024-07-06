package io.vacco.a4lb.impl;

import com.google.gson.Gson;
import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.util.*;
import org.buildobjects.process.ProcBuilder;
import org.slf4j.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

import static io.vacco.a4lb.util.A4Logging.onError;
import static io.vacco.a4lb.util.A4Valid.validationsOf;

public class A4Discover implements Callable<List<A4Backend>> {

  private static final Logger log = LoggerFactory.getLogger(A4Discover.class);

  private final String serverId;
  private final A4Match match;
  private final Gson gson;

  public A4Discover(String serverId, A4Match match, Gson gson) {
    this.serverId = Objects.requireNonNull(serverId);
    this.match = Objects.requireNonNull(match);
    this.gson = Objects.requireNonNull(gson);
    Objects.requireNonNull(match.discover);
  }

  private A4Backend parseLine(String line) {
    var parts = line.split(" ");
    var sck = new A4Sock().host(parts[0]).port(Integer.parseInt(parts[1]));
    var bk = new A4Backend().addr(sck);
    if (parts.length == 4) {
      bk = bk.weight(Integer.parseInt(parts[2])).priority(Integer.parseInt(parts[3]));
    }
    return bk;
  }

  private List<A4Backend> parseLines(Stream<String> lines) {
    return lines
      .map(String::trim)
      .filter(line -> !line.isEmpty())
      .map(this::parseLine)
      .collect(Collectors.toList());
  }

  private List<A4Backend> parsePlainText(String out) {
    return parseLines(Arrays.stream(out.split("\\R")));
  }

  private List<A4Backend> parseOutput(String out, A4Format format) {
    if (format == A4Format.json) {
      var pool = gson.fromJson(out, A4Pool.class);
      return pool.hosts;
    } else if (format == A4Format.text) {
      return parsePlainText(out);
    }
    throw new IllegalArgumentException("Invalid output format " + format);
  }

  private List<A4Backend> execDiscover() {
    var d = match.discover;
    var x = match.discover.exec;
    var result = new ProcBuilder(x.command, x.args).withTimeoutMillis(d.timeoutMs).run();
    return parseOutput(result.getOutputString(), x.format);
  }

  private List<A4Backend> httpDiscover() {
    try {
      var content = A4Io.loadContent(new URI(match.discover.http.endpoint), match.discover.timeoutMs);
      return parseOutput(content, match.discover.http.format);
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  private List<A4Backend> discover() {
    if (match.discover.exec != null) {
      return execDiscover();
    } else if (match.discover.http != null) {
      return httpDiscover();
    }
    log.error("{} - missing discovery method", serverId);
    return Collections.emptyList();
  }

  private List<A4Backend> discoverValid() {
    var bkl = new ArrayList<A4Backend>();
    for (var bk : discover()) {
      if (match.pool.type == A4Pool.Type.weight) {
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
      onError(log, "{} - backend discovery error for match [{}]", e, serverId, match);
    }
    return null;
  }

}
