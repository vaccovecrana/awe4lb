package io.vacco.a4lb.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.sel.A4Sel;
import org.buildobjects.process.ProcBuilder;
import org.slf4j.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class A4Discover implements Callable<Void> {

  private static final Logger log = LoggerFactory.getLogger(A4Discover.class);
  private static final Type TBkList = new TypeToken<ArrayList<A4Backend>>(){}.getType();

  private final ExecutorService bkExec;
  private final A4Match match;
  private final A4Sel bkSel;
  private final Gson gson;

  public A4Discover(A4Match match, A4Sel bkSel, Gson gson, ExecutorService bkExec) {
    this.bkExec = Objects.requireNonNull(bkExec);
    this.match = Objects.requireNonNull(match);
    this.bkSel = Objects.requireNonNull(bkSel);
    this.gson = Objects.requireNonNull(gson);
    Objects.requireNonNull(match.discover);
  }

  private List<A4Backend> discover() {
    if (match.discover.exec != null) {
      var d = match.discover;
      var x = match.discover.exec;
      var result = new ProcBuilder(x.command, x.args).withTimeoutMillis(d.timeoutMs).run();
      if (x.format == A4Format.Json) {
        var out = result.getOutputString();
        return new ArrayList<>(gson.fromJson(out, TBkList));
      } else {
        // TODO implement plaintext parsing
      }
    } else if (match.discover.http != null) {
      // TODO implement this
    }
    return Collections.emptyList();
  }

  private List<A4Backend> validate(List<A4Backend> backends) {
    for (var bk : backends) {
      var errors = A4Valid.A4BackendVld.validate(bk);
      if (!errors.isEmpty()) {
        throw new A4Exceptions.A4ConfigException(errors);
      }
    }
    return backends;
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

  private Void update() {
    try {
      var bkl = validate(discover());
      return bkSel.lockPoolAnd(match.pool, () -> {
        int h0 = hashOf(bkl);
        int h1 = hashOf(match.pool.hosts);
        if (h0 != h1) {
          match.pool.hosts = bkl;
          if (log.isDebugEnabled()) {
            log.debug("Updated backend list for match [{}] - {}", match, bkl);
          }
        }
        return null;
      });
    } catch (Exception e) {
      log.error("Backend update error", e);
      return null;
    }
  }

  @Override public Void call() {
    while (true) {
      try {
        bkExec.invokeAll(List.of(this::update), match.discover.timeoutMs, TimeUnit.MILLISECONDS);
        Thread.sleep(match.discover.intervalMs);
      } catch (Exception e) {
        if (log.isDebugEnabled()) {
          log.error("Backend discovery failed for match [{}]", match, e);
        } else {
          log.warn("Backend discovery failed for match [{}] - {}", match, e.getMessage());
        }
      }
    }
  }

}
