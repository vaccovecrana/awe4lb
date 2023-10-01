package io.vacco.a4lb.tcp;

import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.sel.A4Sel;
import io.vacco.a4lb.util.A4Exceptions;
import org.slf4j.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class A4TcpHealth implements Callable<Void> {

  private static final Logger log = LoggerFactory.getLogger(A4TcpHealth.class);

  private final ExecutorService hltEx;
  private final String serverId;
  private final A4Match match;
  private final A4Sel bkSel;

  public A4TcpHealth(ExecutorService hltEx, String serverId, A4Match match, A4Sel bkSel) {
    this.hltEx = Objects.requireNonNull(hltEx);
    this.serverId = Objects.requireNonNull(serverId);
    this.match = Objects.requireNonNull(match);
    this.bkSel = Objects.requireNonNull(bkSel);
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
        var hcTasks = new ArrayList<Callable<Void>>();
        bkSel.lockPoolAnd(match.pool, () -> {
          for (var bk : match.pool.hosts) {
            hcTasks.add(() -> {
              bk.state = stateOf(bk, match.healthCheck.timeoutMs);
              return null;
            });
          }
        });
        hltEx.invokeAll(hcTasks);
        Thread.sleep(match.healthCheck.intervalMs);
      } catch (Exception e) {
        log.warn("Health check failed for server pool {} {}", serverId, match.pool.hosts, e);
      }
    }
  }

}
