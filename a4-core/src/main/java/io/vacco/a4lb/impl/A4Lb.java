package io.vacco.a4lb.impl;

import com.google.gson.Gson;
import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.sel.A4BackendContext;
import io.vacco.a4lb.sel.A4Selector;
import io.vacco.a4lb.tcp.*;
import io.vacco.a4lb.udp.A4UdpSrv;
import io.vacco.a4lb.util.*;
import org.slf4j.*;
import java.io.Closeable;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public class A4Lb implements Closeable {

  private static final Logger log = LoggerFactory.getLogger(A4Lb.class);

  public  final A4Config    config;
  private final A4Scheduler scheduler;
  private final Gson        gson;

  private final Map<String, A4Selector> selectors = new LinkedHashMap<>();
  private A4StatsRecorder statsRecorder;

  public A4Lb(A4Config config, Gson gson) {
    this.gson = Objects.requireNonNull(gson);
    this.config = Objects.requireNonNull(config);
    A4Valid.validateOrFail(config);
    this.scheduler = new A4Scheduler(config.servers.size() * 2, config.id);
  }

  private String healthCheckTaskIdOf(A4Server srv, A4Match m, A4Backend bk) {
    return format("%s-%s-%s", srv.id,
      Integer.toHexString(m.hashCode()),
      Integer.toHexString(bk.hashCode())
    );
  }

  private void cancelHealthCheck(A4Server srv, A4Match match) {
    for (var bk : match.pool.hosts) {
      scheduler.cancel(healthCheckTaskIdOf(srv, match, bk));
    }
  }

  private void initHealthCheck(A4Server srv, A4Match match) {
    if (match.healthCheck != null) {
      for (var bk : match.pool.hosts) {
        scheduler.scheduleFixed(
          healthCheckTaskIdOf(srv, match, bk),
          match.healthCheck.intervalMs, TimeUnit.MILLISECONDS,
          new A4Health(srv.id, match.healthCheck, bk), bk::state
        );
      }
    }
  }

  private void initDiscover(A4Server srv, A4Match match, int matchIdx, A4Selector bkSel) {
    if (match.discover != null) {
      var dsId = format("%s-match[%d]-discover", srv.id, matchIdx);
      scheduler.scheduleFixed(
        dsId, match.discover.intervalMs, TimeUnit.MILLISECONDS,
        new A4Discover(srv.id, match, gson), bkl1 -> {
          if (bkl1 != null) {
            this.cancelHealthCheck(srv, match);
            match.pool.hosts(bkl1);
            this.initHealthCheck(srv, match);
            var keep = new HashSet<A4Backend>();
            for (var m : srv.match) {
              if (m.pool != null && m.pool.hosts != null) {
                keep.addAll(m.pool.hosts);
              }
            }
            bkSel.retainContexts(keep);
          }
        }
      );
    } else {
      initHealthCheck(srv, match);
    }
  }

  private void initUdp(A4Server srv, A4Selector bkSel) {
    var udpImpl = new A4UdpSrv(A4Io.newSelector(), srv, bkSel);
    var taskId = format("%s-udp-cleanup", srv.id);
    scheduler.schedulePermanent(udpImpl);
    scheduler.scheduleFixed(taskId,
      srv.udp.idleTimeoutMs, TimeUnit.MILLISECONDS,
      udpImpl.createSessionCleanupTask(),
      expiredSessions -> {
        if (!expiredSessions.isEmpty() && log.isDebugEnabled()) {
          log.debug("{} - expired [{}] UDP sessions", taskId, expiredSessions.size());
        }
      }
    );
  }

  public A4Lb open() {
    log.info("{} - starting", config.id);
    try {
      for (var srv : config.servers) {
        var bkSel = new A4Selector(srv.match);
        selectors.put(srv.id, bkSel);
        if (srv.udp != null) {
          this.initUdp(srv, bkSel);
        } else {
          var tcpImpl = new A4TcpSrv(srv, bkSel);
          scheduler.schedulePermanent(tcpImpl);
        }
        for (int i = 0; i < srv.match.size(); i++) {
          this.initDiscover(srv, srv.match.get(i), i, bkSel);
        }
      }
      this.statsRecorder = new A4StatsRecorder(this::snapshot);
      scheduler.scheduleFixed(
        format("%s-stats", config.id),
        A4StatsRecorder.DefaultIntervalMs, TimeUnit.MILLISECONDS,
        () -> statsRecorder.tick(), r -> {}
      );
      this.scheduler.start();
      log.info("{} - started", config.id);
      return this;
    } catch (RuntimeException e) {
      this.scheduler.stop();
      throw e;
    }
  }

  public A4Stats stats() {
    var recorder = this.statsRecorder;
    return recorder != null ? recorder.current() : snapshot();
  }

  /**
   * Builds a point-in-time view of every configured backend. The returned
   * objects are new on every call, so callers can serialize them safely while
   * proxy threads keep updating the underlying counters.
   */
  public A4Stats snapshot() {
    var stats = new A4Stats();
    stats.configId = config.id;
    stats.timestampMs = System.currentTimeMillis();
    for (var srv : config.servers) {
      var selector = selectors.get(srv.id);
      if (selector == null) {
        continue;
      }
      var serverType = srv.udp != null ? "UDP" : srv.tls != null ? "TLS" : "TCP";
      for (int mIdx = 0; mIdx < srv.match.size(); mIdx++) {
        var match = srv.match.get(mIdx);
        if (match.pool == null || match.pool.hosts == null) {
          continue;
        }
        for (var bk : match.pool.hosts) {
          var ctx = selector.contextOf(bk);
          var sb = new A4StatsBackend();
          sb.id = backendIdOf(srv, mIdx, bk);
          sb.serverId = srv.id;
          sb.serverType = serverType;
          sb.matchLabel = match.matchLabel();
          sb.poolType = match.pool.type != null ? match.pool.type.name() : "random";
          sb.host = bk.addr != null ? bk.addr.host : null;
          sb.port = bk.addr != null ? bk.addr.port : 0;
          sb.state = bk.state != null ? bk.state.name() : A4BackendState.Unknown.name();
          fillStats(sb, ctx);
          stats.backends.add(sb);
        }
      }
    }
    return stats;
  }

  private static String backendIdOf(A4Server srv, int matchIdx, A4Backend bk) {
    return format("%s|%d|%s:%d", srv.id, matchIdx,
      bk.addr != null ? bk.addr.host : "?", bk.addr != null ? bk.addr.port : 0);
  }

  private static void fillStats(A4StatsBackend sb, A4BackendContext ctx) {
    sb.activeConnections = ctx.activeConnections();
    sb.totalConnections = ctx.totalConnections();
    sb.rxBytes = ctx.rxBytes();
    sb.txBytes = ctx.txBytes();
  }

  @Override public void close() {
    this.statsRecorder = null;
    this.selectors.clear();
    this.scheduler.stop();
  }

}
