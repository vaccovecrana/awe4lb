package io.vacco.a4lb.tcp;

import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.util.A4Exceptions;
import org.slf4j.*;
import java.net.*;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class A4TcpHealth implements Callable<Void> {

  private static final Logger log = LoggerFactory.getLogger(A4TcpHealth.class);

  private final ExecutorService hltEx;
  private final A4Server srv;
  private final String srvId;

  public A4TcpHealth(ExecutorService hltEx, String srvId, A4Server srv) {
    this.hltEx = Objects.requireNonNull(hltEx);
    this.srv = Objects.requireNonNull(srv);
    this.srvId = Objects.requireNonNull(srvId);
  }

  public A4Backend.State stateOf(A4Backend bk, int timeOutMs) {
    try {
      var socket = new Socket();
      socket.connect(new InetSocketAddress(bk.addr.host, bk.addr.port), timeOutMs);
      socket.close();
      return A4Backend.State.Up;
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        var x = A4Exceptions.rootCauseOf(e);
        log.debug("{} - TCP health check failed - {} - {}", bk, x.getClass().getSimpleName(), x.getMessage());
      }
      return A4Backend.State.Down;
    }
  }

  @Override public Void call() {
    while (true) {
      try {
        hltEx.invokeAll(
            srv.allBackends()
                .map(bk -> (Callable<Void>) () -> {
                  bk.state = stateOf(bk, srv.healthCheck.timeoutMs);
                  return null;
                }).collect(Collectors.toList())
        );
        Thread.sleep(srv.healthCheck.intervalMs);
      } catch (Exception e) {
        log.warn("Health check failed for server " + srvId, e);
      }
    }
  }

}
