package io.vacco.a4lb.cfg;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validator;
import static am.ik.yavi.builder.ValidatorBuilder.*;

public class A4Valid {

  public static final Validator<A4Exec> A4ExecVld = ValidatorBuilder.<A4Exec>of()
      .constraint(
          (ToCharSequence<A4Exec, String>) ex -> ex.command, "command",
          c -> c.notNull().notEmpty().notBlank()
      ).constraint(
          (ToCharSequence<A4Exec, String>) ex -> ex.passOutput, "passOutput",
          c -> c.notNull().notEmpty().notBlank()
      ).constraint(
          (ToCharSequence<A4Exec, String>) ex -> ex.failOutput, "failOutput",
          c -> c.notNull().notEmpty().notBlank()
      ).build();

  public static final Validator<A4HealthCheck> A4HealthCheckVld = ValidatorBuilder.<A4HealthCheck>of()
      .constraint(
          (ToLong<A4HealthCheck>) hc -> hc.intervalMs, "intervalMs",
          c -> c.greaterThan(0L)
      ).constraint(
          (ToLong<A4HealthCheck>) hc -> hc.timeoutMs, "timeoutMs",
          c -> c.greaterThan(0L)
      ).constraintOnTarget(
          hc -> hc.timeoutMs < hc.intervalMs, "timeoutMs",
          "timeoutMs.isLessThanInterval", "\"timeoutMs\" must be less than \"intervalMs\""
      ).nestIfPresent(
          hc -> hc.exec, "exec", A4ExecVld
      ).build();

  public static final Validator<A4Sock> A4SockVld = ValidatorBuilder.<A4Sock>of()
      .constraint(
          (ToInteger<A4Sock>) s -> s.port, "port",
          c -> c.greaterThan(0).lessThanOrEqual(65535)
      ).constraint(
          (ToCharSequence<A4Sock, String>) s -> s.host, "host",
          c -> c.notNull().notBlank().notEmpty()
      ).build();

  public static final Validator<A4Service> A4ServiceVld = ValidatorBuilder.<A4Service>of()
      .nestIfPresent(s -> s.addr, "addr", A4SockVld)
      .nestIfPresent(s -> s.healthCheck, "healthCheck", A4HealthCheckVld)
      .build();

}
