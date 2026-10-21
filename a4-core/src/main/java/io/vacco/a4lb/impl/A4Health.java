package io.vacco.a4lb.impl;

import io.vacco.a4lb.cfg.A4Backend;
import io.vacco.a4lb.cfg.A4BackendState;
import io.vacco.a4lb.cfg.A4HealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.Callable;

import static io.vacco.a4lb.util.A4Logging.onError;

public class A4Health implements Callable<A4BackendState> {

  private static final Logger log = LoggerFactory.getLogger(A4Health.class);

  private final String serverId;
  private final A4Backend backend;
  private final A4HealthCheck healthCheck;

  public A4Health(String serverId, A4HealthCheck healthCheck, A4Backend backend) {
    this.serverId = Objects.requireNonNull(serverId);
    this.healthCheck = Objects.requireNonNull(healthCheck);
    this.backend = Objects.requireNonNull(backend);
  }

  @Override
  public A4BackendState call() {
    try {
      return A4HealthState.stateOf(serverId, backend, healthCheck);
    } catch (Exception e) {
      onError(log, "{} - health check failed - {}", e, serverId, backend);
    }
    return A4BackendState.Unknown;
  }

}
