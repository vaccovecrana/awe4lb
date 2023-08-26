package io.vacco.a4lb.util;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.constraint.CharSequenceConstraint;
import am.ik.yavi.constraint.IntegerConstraint;
import am.ik.yavi.constraint.LongConstraint;
import am.ik.yavi.core.Validator;
import io.vacco.a4lb.cfg.*;

import static am.ik.yavi.builder.ValidatorBuilder.*;

public class A4Valid {

  public static <T> LongConstraint<T> gt0(LongConstraint<T> c) {
    return c.greaterThan(0L);
  }

  public static <T> IntegerConstraint<T> gtLtEq(IntegerConstraint<T> c, int gt, int ltEq) {
    return c.greaterThan(gt).lessThanOrEqual(ltEq);
  }

  public static <T> CharSequenceConstraint<T, String> nnNeNb(CharSequenceConstraint<T, String> c) {
    return c.notNull().notBlank().notEmpty();
  }

  public static final Validator<A4Exec> A4ExecVld = ValidatorBuilder.<A4Exec>of()
      .constraint((ToCharSequence<A4Exec, String>) ex -> ex.command, "command", A4Valid::nnNeNb)
      .constraint((ToCharSequence<A4Exec, String>) ex -> ex.passOutput, "passOutput", A4Valid::nnNeNb)
      .constraint((ToCharSequence<A4Exec, String>) ex -> ex.failOutput, "failOutput", A4Valid::nnNeNb)
      .build();

  public static final Validator<A4HealthCheck> A4HealthCheckVld = ValidatorBuilder.<A4HealthCheck>of()
      .constraint((ToLong<A4HealthCheck>) hc -> hc.intervalMs, "intervalMs", A4Valid::gt0)
      .constraint((ToLong<A4HealthCheck>) hc -> hc.timeoutMs, "timeoutMs", A4Valid::gt0)
      .constraintOnTarget(
          hc -> hc.timeoutMs < hc.intervalMs, "timeoutMs",
          "timeoutMs.isLessThanInterval", "\"timeoutMs\" must be less than \"intervalMs\""
      ).nestIfPresent(hc -> hc.exec, "exec", A4ExecVld)
      .build();

  public static final Validator<A4Sock> A4SockVld = ValidatorBuilder.<A4Sock>of()
      .constraint((ToInteger<A4Sock>) s -> s.port, "port", c -> gtLtEq(c, 0, 65535))
      .constraint((ToCharSequence<A4Sock, String>) s -> s.host, "host", A4Valid::nnNeNb)
      .build();

  public static final Validator<A4Service> A4ServiceVld = ValidatorBuilder.<A4Service>of()
      .nest(s -> s.addr, "addr", A4SockVld)
      .nest(s -> s.healthCheck, "healthCheck", A4HealthCheckVld)
      .build();

  public static final Validator<A4Backend> A4BackendVld = ValidatorBuilder.<A4Backend>of()
      .constraint((ToInteger<A4Backend>) b -> b.weight, "weight", c -> gtLtEq(c, 1, 100))
      .constraint((ToInteger<A4Backend>) b -> b.priority, "priority", c -> gtLtEq(c, 1, 100))
      .nest(b -> b.addr, "addr", A4SockVld)
      .build();

}
