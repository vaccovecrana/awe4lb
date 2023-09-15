package io.vacco.a4lb.util;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.constraint.*;
import am.ik.yavi.core.Validator;
import io.vacco.a4lb.cfg.*;
import java.util.Arrays;
import java.util.Objects;

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

  public static boolean arrayValsNnLtEq(int ltEq, Object ... vals) {
    return Arrays.stream(vals).filter(Objects::nonNull).count() <= ltEq;
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
          "timeoutMs.isLessThanInterval",
          "\"{0}\" \"timeoutMs\" must be less than \"intervalMs\""
      ).nestIfPresent(hc -> hc.exec, "exec", A4ExecVld)
      .build();

  public static final Validator<A4Sock> A4SockVld = ValidatorBuilder.<A4Sock>of()
      .constraint((ToInteger<A4Sock>) s -> s.port, "port", c -> gtLtEq(c, 0, 65535))
      .constraint((ToCharSequence<A4Sock, String>) s -> s.host, "host", A4Valid::nnNeNb)
      .build();

  public static final Validator<A4Backend> A4BackendVld = ValidatorBuilder.<A4Backend>of()
      .constraint((ToInteger<A4Backend>) b -> b.weight, "weight", c -> gtLtEq(c, 0, 100))
      .constraint((ToInteger<A4Backend>) b -> b.priority, "priority", c -> gtLtEq(c, 0, 100))
      .constraintOnCondition(
          (b, cg) -> b.weight != null || b.priority != null,
          b -> b.constraintOnTarget(
              b0 -> b0.weight != null && b0.priority != null,
              "weightPriotity", "weightPriority.allOf",
              "\"{0}\" must specify [weight, priority]"
          )
      ).nest(b -> b.addr, "addr", A4SockVld)
      .build();

  public static final Validator<A4StringOp> A4StringOpVld = ValidatorBuilder.<A4StringOp>of()
      .constraintOnCondition(
          (op, cg) -> op.equals != null,
          b -> b.constraint((ToCharSequence<A4StringOp, String>) op -> op.equals, "equals", A4Valid::nnNeNb)
      ).constraintOnCondition(
          (op, cg) -> op.contains != null,
          b -> b.constraint((ToCharSequence<A4StringOp, String>) op -> op.contains, "contains", A4Valid::nnNeNb)
      ).constraintOnCondition(
          (op, cg) -> op.startsWith != null,
          b -> b.constraint((ToCharSequence<A4StringOp, String>) op -> op.startsWith, "startsWith", A4Valid::nnNeNb)
      ).constraintOnCondition(
          (op, cg) -> op.endsWith != null,
          b -> b.constraint((ToCharSequence<A4StringOp, String>) op -> op.endsWith, "endsWith", A4Valid::nnNeNb)
      ).constraintOnTarget(
          op -> op.equals != null || op.contains != null || op.startsWith != null || op.endsWith != null,
          "stringOp", "stringOp.anyOf",
          "\"{0}\" missing any of [equals, contains, startsWith, endsWith]"
      ).constraintOnTarget(
          op -> arrayValsNnLtEq(1, op.equals, op.contains, op.startsWith, op.endsWith),
          "stringOp", "stringOp.oneOf",
          "\"{0}\" only one of [equals, contains, startsWith, endsWith] allowed"
      ).build();

  public static final Validator<A4MatchOp> A4MatchOpVld = ValidatorBuilder.<A4MatchOp>of()
      .nestIfPresent(mo -> mo.host, "host", A4StringOpVld)
      .nestIfPresent(mo -> mo.sni, "sni", A4StringOpVld)
      .constraintOnTarget(
          mo -> mo.host != null || mo.sni != null,
          "matchOp", "matchOp.anyOf", "\"{0}\" missing any of [host, sni]"
      ).constraintOnTarget(
          mo -> arrayValsNnLtEq(1, mo.host, mo.sni),
          "matchOp", "matchOp.oneOf",
          "\"{0}\" only one of [host, sni] allowed"
      ).build();

  public static final Validator<A4Pool> A4PoolVld = ValidatorBuilder.<A4Pool>of()
      .constraint(A4Pool::hostList, "hosts", c -> c.notNull().notEmpty())
      .forEach(A4Pool::hostList, "hosts", A4BackendVld)
      .build();

  public static final Validator<A4Match> A4MatchVld = ValidatorBuilder.<A4Match>of()
      .constraintOnCondition(
          (m, cg) -> m.and != null,
          b -> b.forEach(A4Match::andOps, "match.andOps", A4MatchOpVld)
      ).constraintOnCondition(
          (m, cg) -> m.or != null,
          b -> b.forEach(A4Match::orOps, "match.orOps", A4MatchOpVld)
      ).constraintOnTarget(
          m -> arrayValsNnLtEq(1, m.and, m.or),
          "matchOps.andOr", "matchOps.andOr.oneOf",
          "\"{0}\" only one of [and, or] allowed"
      ).nest(
          m -> m.pool, "pool", A4PoolVld
      ).build();

  public static final Validator<A4Tls> A4TlsVld = ValidatorBuilder.<A4Tls>of()
      .constraint((ToCharSequence<A4Tls, String>) t -> t.certPath, "certPath", A4Valid::nnNeNb)
      .constraint((ToCharSequence<A4Tls, String>) t -> t.keyPath, "keyPath", A4Valid::nnNeNb)
      .forEach(A4Tls::tlsVersionList, "tlsVersions.version",
          svb -> svb.constraint((ToCharSequence<String, String>) s -> s, "value", A4Valid::nnNeNb)
      )
      .forEach(A4Tls::cipherList, "ciphers.cipher",
          svb -> svb.constraint((ToCharSequence<String, String>) s -> s, "value", A4Valid::nnNeNb)
      )
      .build();

  public static final Validator<A4Server> A4ServerVld = ValidatorBuilder.<A4Server>of()
      .nest(s -> s.addr, "addr", A4SockVld)
      .nest(s -> s.healthCheck, "healthCheck", A4HealthCheckVld)
      .nestIfPresent(s -> s.tls, "tls", A4TlsVld)
      .constraint(A4Server::matchList, "match", c -> c.notNull().notEmpty())
      .constraint((ToInteger<A4Server>) s -> s.bufferSize, "bufferSize", c -> c.greaterThan(0))
      .forEach(A4Server::matchList, "match", A4MatchVld)
      .build();

  public static final Validator<A4Probe> A4ProbeVld = ValidatorBuilder.<A4Probe>of()
      .nest(p -> p.bind, "bind", A4SockVld)
      .build();

  public static final Validator<A4Config> A4ConfigVld = ValidatorBuilder.<A4Config>of()
      .nest(c -> c.api, "api", A4ProbeVld)
      .nest(c -> c.metrics, "metrics", A4ProbeVld)
      .constraint(A4Config::serverIdx, "servers", c -> c.notNull().notEmpty())
      .forEach(A4Config::serverIdx, "servers", A4ServerVld)
      .build();

  /*
   * A director needs a team. My management team. These people know the secrets of the Bureau
   * as well as I do, some even better.
   *
   * They have proven themselves. Darling, Tommasi, Salvador. Marshall. Marshall especially,
   * my Head of Operations. She sees right through me. She knows I don't like relying on people.
   * The only person you should fail is yourself. So I followed my own orders.
   *
   * Northmoor hated my guts for that. But things change when you become Director. You suddenly
   * see this dark void for the horror show it truly is, filled with screaming fear we pretend to control.
   *
   * Sand leaks through my fingers. The roses I pruned in the garden, back when I still had a family,
   * all dead. Heat escapes my body. My thoughts are scattered. The universe keeps expanding.
   * I can't keep up with it alone. When I forget that, things go wrong. And my team has to pick up the pieces.
   *
   * Damage control, to help me get out of my mess.
   */

}
