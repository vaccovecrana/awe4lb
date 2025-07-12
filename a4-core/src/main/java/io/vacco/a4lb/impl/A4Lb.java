package io.vacco.a4lb.impl;

import com.google.gson.Gson;
import io.vacco.a4lb.cfg.*;
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

  private void initDiscover(A4Server srv, A4Match match, int matchIdx) {
    if (match.discover != null) {
      var dsId = format("%s-match[%d]-discover", srv.id, matchIdx);
      scheduler.scheduleFixed(
        dsId, match.discover.intervalMs, TimeUnit.MILLISECONDS,
        new A4Discover(srv.id, match, gson), bkl1 -> {
          if (bkl1 != null) {
            this.cancelHealthCheck(srv, match);
            match.pool.hosts(bkl1);
            this.initHealthCheck(srv, match);
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
    for (var srv : config.servers) {
      var bkSel = new A4Selector(srv.match);
      if (srv.udp != null) {
        this.initUdp(srv, bkSel);
      } else {
        var tcpImpl = new A4TcpSrv(srv, bkSel);
        scheduler.schedulePermanent(tcpImpl);
      }
      for (int i = 0; i < srv.match.size(); i++) {
        this.initDiscover(srv, srv.match.get(i), i);
      }
    }
    this.scheduler.start();
    log.info("{} - started", config.id);
    return this;
  }

  @Override public void close() {
    this.scheduler.stop();
  }

}
