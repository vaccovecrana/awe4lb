package io.vacco.a4lb.impl;

import com.google.gson.Gson;
import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.tcp.*;
import io.vacco.a4lb.util.*;
import org.slf4j.*;
import java.io.Closeable;
import java.util.*;
import java.util.concurrent.*;

public class A4Lb implements Closeable {

  private static final Logger log = LoggerFactory.getLogger(A4Lb.class);

  public  final A4Config        config;
  private final Gson            gson;
  private final ExecutorService exSvc;
  private final List<A4TcpSrv>  servers = new ArrayList<>();

  public A4Lb(A4Config config, Gson gson) {
    this.gson = Objects.requireNonNull(gson);
    this.config = Objects.requireNonNull(config);
    A4Valid.validateOrFail(config);
    this.exSvc = Executors.newCachedThreadPool(new A4ThreadFactory(String.format("a4lb-%s", config.id)));
  }

  public A4Lb open() {
    log.info("{} - starting", config.id);
    for (var srv : config.servers) {
      var srvImpl = new A4TcpSrv(A4Io.newSelector(), srv, exSvc);
      // TODO this will need to accommodate UDP servers too.
      servers.add(srvImpl);
      exSvc.submit(srvImpl);
      for (var match : srv.match) {
        if (srv.udp == null && match.healthCheck == null) {
          log.info("{} - {} - no TCP health check configuration specified. Using defaults.", srv.id, match);
          match.healthCheck = new A4HealthCheck();
        }
        if (match.healthCheck != null) {
          exSvc.submit(new A4Health(exSvc, srv.id, match, srvImpl.bkSel));
        }
        if (match.discover != null) {
          exSvc.submit(new A4Discover(srv.id, match, srvImpl.bkSel, gson, exSvc));
        }
      }
    }
    log.info("{} - started", config.id);
    return this;
  }

  @Override public void close() {
    exSvc.shutdownNow();
    servers.forEach(A4TcpSrv::close); // TODO accommodate UDP servers too
    log.info("{} - stopped", config.id);
  }

}
