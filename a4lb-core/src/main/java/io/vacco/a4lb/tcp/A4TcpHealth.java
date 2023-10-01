package io.vacco.a4lb.tcp;

import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.sel.A4Selector;
import org.slf4j.*;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class A4TcpHealth implements Callable<Void> {

  private static final Logger log = LoggerFactory.getLogger(A4TcpHealth.class);

  private final ExecutorService exSvc;
  private final String serverId;
  private final A4Match match;
  private final A4Selector bkSel;

  public A4TcpHealth(ExecutorService exSvc, String serverId, A4Match match, A4Selector bkSel) {
    this.exSvc = Objects.requireNonNull(exSvc);
    this.serverId = Objects.requireNonNull(serverId);
    this.match = Objects.requireNonNull(match);
    this.bkSel = Objects.requireNonNull(bkSel);
  }

  @Override public Void call() {
    while (true) {
      try {
        var tasks = bkSel.lockPoolAnd(match.pool,
            () -> match.pool.hosts.stream()
                .map(bk -> (Callable<A4Backend>) () -> {
                  if (match.healthCheck.exec != null) {
                    return bk.state(A4Backend.State.Unknown); // TODO implement this
                  }
                  return bk.state(A4Io.stateOf(bk, match.healthCheck.timeoutMs));
                }).collect(Collectors.toList())
        );
        exSvc.invokeAll(tasks);
        Thread.sleep(match.healthCheck.intervalMs);
      } catch (Exception e) {
        log.warn("Health check failed for server pool {} {}", serverId, match.pool.hosts, e);
      }
    }
  }

}
