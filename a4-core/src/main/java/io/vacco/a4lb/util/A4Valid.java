package io.vacco.a4lb.util;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.constraint.*;
import am.ik.yavi.constraint.base.ContainerConstraintBase;
import am.ik.yavi.core.Constraint;
import am.ik.yavi.core.ConstraintViolations;
import am.ik.yavi.core.Validator;
import io.vacco.a4lb.cfg.*;
import java.util.*;

public class A4Valid {

  public static <T> IntegerConstraint<T> gtZero(IntegerConstraint<T> c) {
    return c.greaterThan(0);
  }

  public static <T> IntegerConstraint<T> gtLtEq(IntegerConstraint<T> c, int gt, int ltEq) {
    return c.greaterThan(gt).lessThanOrEqual(ltEq);
  }

  public static <T> CharSequenceConstraint<T, String> nnNeNb(CharSequenceConstraint<T, String> c) {
    return c.notNull().notBlank().notEmpty();
  }

  public static boolean allNoNullLtEq(int ltEq, Object ... vals) {
    return Arrays.stream(vals).filter(Objects::nonNull).count() <= ltEq;
  }

  private static final Validator<A4HealthExec> A4HealthExecVld = ValidatorBuilder.<A4HealthExec>of()
      ._string(ex -> ex.command, "command", A4Valid::nnNeNb)
      .forEach(A4HealthExec::argList, "args", b -> b._string(s -> s, "arg", A4Valid::nnNeNb))
      .build();

  private static final Validator<A4HealthCheck> A4HealthCheckVld = ValidatorBuilder.<A4HealthCheck>of()
      ._integer(hc -> hc.intervalMs, "intervalMs", A4Valid::gtZero)
      ._integer(hc -> hc.timeoutMs, "timeoutMs", A4Valid::gtZero)
      .constraintOnTarget(
          hc -> hc.timeoutMs < hc.intervalMs, "timeoutMs",
          "timeoutMs.isLessThanInterval",
          "\"{0}\" \"timeoutMs\" must be less than \"intervalMs\""
      ).nestIfPresent(hc -> hc.exec, "exec", A4HealthExecVld)
      .build();

  private static final Validator<A4Sock> A4SockVld = ValidatorBuilder.<A4Sock>of()
      ._integer(s -> s.port, "port", c -> gtLtEq(c, 0, 65535))
      ._string(s -> s.host, "host", A4Valid::nnNeNb)
      .build();

  private static final Validator<A4Backend> A4BackendVld = ValidatorBuilder.<A4Backend>of()
      ._integer(b -> b.weight, "weight", c -> gtLtEq(c, 0, 100))
      ._integer(b -> b.priority, "priority", c -> gtLtEq(c, 0, 100))
      .constraintOnCondition(
          (b, cg) -> b.weight != null || b.priority != null,
          b -> b.constraintOnTarget(
              b0 -> b0.weight != null && b0.priority != null,
              "weightPriority", "weightPriority.allOf",
              "\"{0}\" must specify all of [weight, priority]"
          )
      ).nest(b -> b.addr, "addr", A4SockVld)
      .build();

  private static final Validator<A4StringOp> A4StringOpVld = ValidatorBuilder.<A4StringOp>of()
      .constraintOnCondition(
          (op, cg) -> op.equals != null,
          b -> b._string(op -> op.equals, "equals", A4Valid::nnNeNb)
      ).constraintOnCondition(
          (op, cg) -> op.contains != null,
          b -> b._string(op -> op.contains, "contains", A4Valid::nnNeNb)
      ).constraintOnCondition(
          (op, cg) -> op.startsWith != null,
          b -> b._string(op -> op.startsWith, "startsWith", A4Valid::nnNeNb)
      ).constraintOnCondition(
          (op, cg) -> op.endsWith != null,
          b -> b._string(op -> op.endsWith, "endsWith", A4Valid::nnNeNb)
      ).constraintOnTarget(
          op -> op.equals != null || op.contains != null || op.startsWith != null || op.endsWith != null,
          "anyOf", "anyOf",
          "\"{0}\" missing any of [equals, contains, startsWith, endsWith]"
      ).constraintOnTarget(
          op -> allNoNullLtEq(1, op.equals, op.contains, op.startsWith, op.endsWith),
          "oneOf", "oneOf",
          "\"{0}\" only one of [equals, contains, startsWith, endsWith] allowed"
      ).build();

  private static final Validator<A4MatchOp> A4MatchOpVld = ValidatorBuilder.<A4MatchOp>of()
      .nestIfPresent(mo -> mo.host, "host", A4StringOpVld)
      .nestIfPresent(mo -> mo.sni, "sni", A4StringOpVld)
      .constraintOnTarget(
          mo -> mo.host != null || mo.sni != null,
          "anyOf", "anyOf",
          "\"{0}\" missing any of [host, sni]"
      ).constraintOnTarget(
          mo -> allNoNullLtEq(1, mo.host, mo.sni),
          "oneOf", "oneOf",
          "\"{0}\" only one of [host, sni] allowed"
      ).build();

  private static final Validator<A4DiscHttp> A4DiscHttpVld = ValidatorBuilder.<A4DiscHttp>of()
      ._object(h -> h.format, "format", Constraint::notNull)
      ._string(h -> h.endpoint, "endpoint", c -> nnNeNb(c).url())
      .build();

  private static final Validator<A4DiscExec> A4DiscExecVld = ValidatorBuilder.<A4DiscExec>of()
      ._object(e -> e.format, "format", Constraint::notNull)
      ._string(e -> e.command, "command", A4Valid::nnNeNb)
      .forEach(A4DiscExec::argList, "args", b -> b._string(s -> s, "arg", A4Valid::nnNeNb))
      .build();

  private static final Validator<A4Disc> A4DiscVld = ValidatorBuilder.<A4Disc>of()
      .nestIfPresent(d -> d.http, "http", A4DiscHttpVld)
      .nestIfPresent(d -> d.exec, "exec", A4DiscExecVld)
      ._integer(d -> d.intervalMs, "intervalMs", A4Valid::gtZero)
      ._integer(d -> d.timeoutMs, "timeoutMs", A4Valid::gtZero)
      .constraintOnTarget(
          d -> d.timeoutMs < d.intervalMs, "timeoutMs",
          "timeoutMs.isLessThanInterval",
          "\"{0}\" \"timeoutMs\" must be less than \"intervalMs\""
      )
      .constraintOnTarget(
          d -> d.http != null || d.exec != null,
          "anyOf", "anyOf",
          "\"{0}\" missing any of [http, exec]"
      ).constraintOnTarget(
          d -> allNoNullLtEq(1, d.http, d.exec),
          "oneOf", "oneOf",
          "\"{0}\" only one of [http, exec] allowed"
      )
      .build();

  private static final Validator<A4Pool> A4PoolVld = ValidatorBuilder.<A4Pool>of()
      .constraint(A4Pool::hostList, "hosts", Constraint::notNull)
      .forEach(A4Pool::hostList, "hosts", A4BackendVld)
      .build();

  private static final Validator<A4Match> A4MatchVld = ValidatorBuilder.<A4Match>of()
      .constraintOnCondition(
          (m, cg) -> m.and != null,
          b -> b.forEach(A4Match::andOps, "match.andOps", A4MatchOpVld)
      ).constraintOnCondition(
          (m, cg) -> m.or != null,
          b -> b.forEach(A4Match::orOps, "match.orOps", A4MatchOpVld)
      ).constraintOnTarget(
          m -> allNoNullLtEq(1, m.and, m.or),
          "oneOf", "oneOf",
          "\"{0}\" only one of [and, or] allowed"
      )
      .constraintOnCondition(
          (m, cg) -> m.discover != null && m.healthCheck != null,
          b -> b.constraintOnTarget(
            m -> m.discover.intervalMs > m.healthCheck.intervalMs, "intervalMs",
              "discover.intervalMs.isGreaterThan.healthCheck.intervalMs",
              "\"{0}\" \"discover.intervalMs\" must be greater than \"healthCheck.intervalMs\""
          )
      )
      .constraintOnCondition(
          (m, cg) -> m.discover == null,
          b -> b.nest(
              m -> m.pool, "pool.hosts",
              b0 -> b0.constraint(A4Pool::hostList, "list", ContainerConstraintBase::notEmpty)
          )
      )
      .nest(m -> m.pool, "pool", A4PoolVld)
      .nestIfPresent(p -> p.discover, "discover", A4DiscVld)
      .nestIfPresent(s -> s.healthCheck, "healthCheck", A4HealthCheckVld)
      .build();

  private static final Validator<A4Tls> A4TlsVld = ValidatorBuilder.<A4Tls>of()
      ._string(t -> t.certPath, "certPath", A4Valid::nnNeNb)
      ._string(t -> t.keyPath, "keyPath", A4Valid::nnNeNb)
      .forEach(A4Tls::protocolList, "protocols.version",
          svb -> svb._string(s -> s, "value", A4Valid::nnNeNb)
      )
      .forEach(A4Tls::cipherList, "ciphers.cipher",
          svb -> svb._string(s -> s, "value", A4Valid::nnNeNb)
      )
      .build();

  private static final Validator<A4Udp> A4UdpVld = ValidatorBuilder.<A4Udp>of()
      ._integer(u -> u.bufferSize, "bufferSize", A4Valid::gtZero)
      .build();

  private static final Validator<A4Server> A4ServerVld = ValidatorBuilder.<A4Server>of()
      ._string(s -> s.id, "id", A4Valid::nnNeNb)
      .nest(s -> s.addr, "addr", A4SockVld)
      .nestIfPresent(s -> s.tls, "tls", A4TlsVld)
      .nestIfPresent(s -> s.udp, "udp", A4UdpVld)
      .constraint(A4Configs::allMatchesOf, "match", c -> c.notNull().notEmpty())
      .forEach(A4Configs::allMatchesOf, "match", A4MatchVld)
      .constraintOnTarget(
          s -> allNoNullLtEq(1, s.tls, s.udp),
          "oneOf", "oneOf",
          "\"{0}\" only one of [tls, udp] allowed"
      )
      .constraintOnCondition(
          (s, cg) -> s.udp != null,
          b ->
              b.forEach(
                  A4Configs::allPoolsOf, "udpPool",
                  pb -> pb.constraintOnTarget(
                      p -> p.type != A4Pool.Type.leastConn, "poolType", "poolType",
                      "\"{0}\" cannot use [leastConn] for UDP backend assignment"
                  )
              ).forEach(
                  A4Configs::allMatchesOf, "udpMatch",
                  mb -> mb.nestIfPresent(
                      m -> m.healthCheck, "healthCheck",
                      hb -> hb.constraintOnTarget(
                          h -> h.exec != null, "exec", "exec",
                          "\"{0}\" only [exec] allowed for UDP health check"
                      )
                  )
              )
      )
      .build();

  private static final Validator<A4Config> A4ConfigVld = ValidatorBuilder.<A4Config>of()
      ._string(c -> c.id, "id", A4Valid::nnNeNb)
      ._string(c -> c.description, "description", A4Valid::nnNeNb)
      .constraint(A4Config::serverList, "servers", c -> c.notNull().notEmpty())
      .forEach(A4Config::serverList, "servers", A4ServerVld)
      .build();

  private static final Validator<A4Flags> A4FlagsVld = ValidatorBuilder.<A4Flags>of()
      ._object(fl -> fl.root, A4Flags.kConfig, Constraint::notNull)
      .constraintOnTarget(
          fl -> fl.root.exists(), A4Flags.kConfig, "exists",
          "\"{0}\" does not exist (not a file or directory)"
      )
      .nest(fl -> fl.api, "--api-*", A4SockVld)
      .build();

  private static final Map<Class<?>, Validator<?>> validators = new HashMap<>();

  static {
    validators.put(A4Backend.class, A4BackendVld);
    validators.put(A4Config.class, A4ConfigVld);
    validators.put(A4Flags.class, A4FlagsVld);
  }

  public static <T> ConstraintViolations validate(T t) {
    @SuppressWarnings("unchecked")
    var validator = (Validator<T>) validators.get(t.getClass());
    if (validator == null) {
      throw new IllegalStateException("Unknown validated type " + t.getClass());
    }
    return validator.validate(t);
  }

  public static <T> T validateOrFail(T t) {
    var errors = validate(t);
    if (!errors.isEmpty()) {
      throw new A4Exceptions.A4ValidationException(errors);
    }
    return t;
  }

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
