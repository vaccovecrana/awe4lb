package io.vacco.a4lb.impl;

import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.sel.A4Selector;
import io.vacco.a4lb.util.*;
import org.slf4j.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class A4Health implements Callable<Void> {

  private static final Logger log = LoggerFactory.getLogger(A4Health.class);

  private final ExecutorService exSvc;
  private final String serverId;
  private final A4Match match;
  private final A4Selector bkSel;

  public A4Health(ExecutorService exSvc, String serverId, A4Match match, A4Selector bkSel) {
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
                .map(bk -> (Callable<A4Backend>) () -> bk.state(A4HealthState.stateOf(serverId, bk, match.healthCheck)))
                .collect(Collectors.toList())
        );
        exSvc.invokeAll(tasks);
        Thread.sleep(match.healthCheck.intervalMs);
      } catch (RejectedExecutionException | InterruptedException e) {
        if (log.isTraceEnabled()) {
          log.trace("{} - health check task stopped", serverId, e);
        }
      } catch (Exception e) {
        if (log.isDebugEnabled()) {
          log.debug("{} - health check failed - {}", serverId, match.pool.hosts, e);
        } else {
          var msg = A4Exceptions.messageFor(e);
          log.warn("{} - health check failed - {} - {}", serverId, match.pool.hosts, msg);
        }
      }
    }
  }

}
