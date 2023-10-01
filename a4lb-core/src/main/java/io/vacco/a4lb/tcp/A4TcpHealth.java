package io.vacco.a4lb.tcp;

import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.sel.A4Sel;
import org.slf4j.*;
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

  @Override public Void call() {
    while (true) {
      try {
        var tasks = bkSel.lockPoolAnd(match.pool,
            () -> match.pool.hosts.stream()
                .map(bk -> (Callable<Void>) () -> {
                  bk.state = A4Io.stateOf(bk, match.healthCheck.timeoutMs);
                  return null;
                }).collect(Collectors.toList())
        );
        hltEx.invokeAll(tasks);
        Thread.sleep(match.healthCheck.intervalMs);
      } catch (Exception e) {
        log.warn("Health check failed for server pool {} {}", serverId, match.pool.hosts, e);
      }
    }
  }

}
