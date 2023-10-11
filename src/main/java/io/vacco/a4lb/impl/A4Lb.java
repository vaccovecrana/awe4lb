package io.vacco.a4lb.impl;

import com.google.gson.Gson;
import io.vacco.a4lb.cfg.A4Config;
import io.vacco.a4lb.tcp.*;
import io.vacco.a4lb.util.*;
import org.slf4j.*;
import java.io.Closeable;
import java.util.*;
import java.util.concurrent.*;

public class A4Lb implements Closeable {

  private static final Logger log = LoggerFactory.getLogger(A4Lb.class);

  private final Gson gson;
  private final A4Config config;
  private final ExecutorService exSvc = Executors.newCachedThreadPool(new A4ThreadFactory("awe4lb"));
  private final List<A4TcpSrv> servers = new ArrayList<>();

  public A4Lb(A4Config config, Gson gson) {
    this.gson = Objects.requireNonNull(gson);
    this.config = Objects.requireNonNull(config);
    var configErrors = A4Valid.A4ConfigVld.validate(config);
    if (!configErrors.isEmpty()) {
      throw new A4Exceptions.A4ConfigException(configErrors);
    }
  }

  public A4Lb open() {
    log.info("{} - starting", config.id);
    for (var srv : config.servers) {
      var srvImpl = new A4TcpSrv(A4Io.newSelector(), srv, exSvc);
      // TODO this will need to accommodate UDP servers too.
      servers.add(srvImpl);
      exSvc.submit(srvImpl);
      for (var match : srv.match) {
        exSvc.submit(new A4TcpHealth(exSvc, srv.id, match, srvImpl.bkSel));
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
