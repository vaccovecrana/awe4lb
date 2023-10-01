package io.vacco.a4lb.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.sel.A4Selector;
import io.vacco.a4lb.tcp.A4Io;
import org.buildobjects.process.ProcBuilder;
import org.slf4j.*;
import java.lang.reflect.Type;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class A4Discover implements Callable<Void> {

  private static final Logger log = LoggerFactory.getLogger(A4Discover.class);
  private static final Type TBkList = new TypeToken<ArrayList<A4Backend>>(){}.getType();

  private final String serverId;
  private final ExecutorService exSvc;
  private final A4Match match;
  private final A4Selector bkSel;
  private final Gson gson;

  public A4Discover(String serverId, A4Match match, A4Selector bkSel, Gson gson, ExecutorService exSvc) {
    this.serverId = Objects.requireNonNull(serverId);
    this.exSvc = Objects.requireNonNull(exSvc);
    this.match = Objects.requireNonNull(match);
    this.bkSel = Objects.requireNonNull(bkSel);
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
    return lines.map(String::trim)
        .filter(line -> line.length() > 0)
        .map(this::parseLine)
        .collect(Collectors.toList());
  }

  private List<A4Backend> parsePlainText(String out) {
    return parseLines(Arrays.stream(out.split("\\R")));
  }

  private List<A4Backend> parseOutput(String out, A4Format format) {
    if (format == A4Format.json) {
      return new ArrayList<>(gson.fromJson(out, TBkList));
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
      var content = A4Io.loadContent(new URL(match.discover.http.endpoint));
      return parseOutput(content, match.discover.http.format);
    } catch (MalformedURLException e) {
      throw new IllegalStateException(e);
    }
  }

  private List<A4Backend> discover() {
    if (match.discover.exec != null) {
      return execDiscover();
    } else if (match.discover.http != null) {
      return httpDiscover();
    }
    return Collections.emptyList();
  }

  public Callable<A4Backend> validateTask(final A4Backend bk, int timeoutMs) {
    return () -> {
      if (log.isDebugEnabled()) {
        log.debug("{} - validating backend {}", serverId, bk);
      }
      if (match.pool.type == A4Pool.Type.Weight) {
        if (bk.weight == null || bk.priority == null) {
          bk.weight(1).priority(1);
          log.warn("{} - backend entry missing weight/priority, assigned defaults - {}", serverId, bk);
        }
      }
      var errors = A4Valid.A4BackendVld.validate(bk);
      if (!errors.isEmpty()) {
        throw new A4Exceptions.A4ConfigException(errors);
      }
      return bk.state(A4Io.stateOf(bk, timeoutMs));
    };
  }

  private List<A4Backend> validate(List<A4Backend> backends) throws InterruptedException {
    var tasks = backends.stream()
        .map(bk -> validateTask(bk, match.discover.timeoutMs))
        .collect(Collectors.toList());
    exSvc.invokeAll(tasks);
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
          log.info("{} - updated backend list for match [{}] - {}", serverId, match, bkl);
        }
        return null;
      });
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.debug("{} - backend update error", serverId, e);
      } else {
        log.warn(
            "{} - backend update error - {}", serverId,
            e.getMessage()  != null ? e.getMessage() : e.getClass().getSimpleName()
        );
      }
      return null;
    }
  }

  @Override public Void call() {
    while (true) {
      try {
        exSvc.invokeAll(List.of(this::update), match.discover.timeoutMs, TimeUnit.MILLISECONDS);
        Thread.sleep(match.discover.intervalMs);
      } catch (Exception e) {
        if (log.isDebugEnabled()) {
          log.error("{} - backend discovery failed for match [{}]", serverId, match, e);
        } else {
          log.warn("{} - backend discovery failed for match [{}] - {}", serverId, match, e.getMessage());
        }
      }
    }
  }

}
