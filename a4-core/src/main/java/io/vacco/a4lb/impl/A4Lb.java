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
import java.util.concurrent.*;

public class A4Lb implements Closeable {

  private static final Logger log = LoggerFactory.getLogger(A4Lb.class);

  public  final A4Config        config;
  private final Gson            gson;
  private final ExecutorService exSvc;
  private final List<Closeable> servers = new ArrayList<>();

  public A4Lb(A4Config config, Gson gson) {
    this.gson = Objects.requireNonNull(gson);
    this.config = Objects.requireNonNull(config);
    A4Valid.validateOrFail(config);
    this.exSvc = Executors.newCachedThreadPool(new A4ThreadFactory(String.format("a4lb-%s", config.id)));
  }

  public A4Lb open() {
    log.info("{} - starting", config.id);
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    for (var srv : config.servers) {
      var bkSel = new A4Selector(srv.match);
      if (srv.udp != null) {
        var udpImpl = new A4UdpSrv(A4Io.newSelector(), srv, bkSel);
        servers.add(udpImpl);
        exSvc.submit(udpImpl);
      } else {
        var tcpImpl = new A4TcpSrv(A4Io.newSelector(), srv, bkSel, exSvc);
        servers.add(tcpImpl);
        exSvc.submit(tcpImpl);
      }
      for (var match : srv.match) {
        if (match.healthCheck != null) {
          exSvc.submit(new A4Health(exSvc, srv.id, match, bkSel));
        }
        if (match.discover != null) {
          exSvc.submit(new A4Discover(srv.id, match, bkSel, gson, exSvc));
        }
      }
    }
    log.info("{} - started", config.id);
    return this;
  }

  @Override public void close() {
    exSvc.shutdownNow();
    servers.forEach(A4Io::close);
    servers.clear();
    log.info("{} - stopped", config.id);
  }

}
