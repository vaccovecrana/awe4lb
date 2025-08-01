package io.vacco.a4lb.util;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.constraint.*;
import am.ik.yavi.constraint.base.ContainerConstraintBase;
import am.ik.yavi.core.*;
import io.vacco.a4lb.cfg.*;
import java.io.File;
import java.net.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static am.ik.yavi.core.NullAs.VALID;
import static am.ik.yavi.core.ViolationMessage.Default.CHAR_SEQUENCE_URL;

public class A4Valid {

  public static String anyOf = "anyOF", oneOf = "oneOf", unique = "unique";

  public static <T> IntegerConstraint<T> gtZero(IntegerConstraint<T> c) {
    return c.notNull().greaterThan(0);
  }

  public static <T> IntegerConstraint<T> gtLtEq(IntegerConstraint<T> c, int gt, int ltEq) {
    return c.greaterThan(gt).lessThanOrEqual(ltEq);
  }

  /**
   * Not null, not empty, not blank.
   *
   * @param c constraint
   * @return constraint
   * @param <T> input type
   */
  public static <T> CharSequenceConstraint<T, String> nnNeNb(CharSequenceConstraint<T, String> c) {
    return c.notNull().notBlank().notEmpty();
  }

  public static boolean allNoNullLtEq(int ltEq, Object ... vals) {
    return Arrays.stream(vals).filter(Objects::nonNull).count() <= ltEq;
  }

  public static <T> CharSequenceConstraint<T, String> uri(CharSequenceConstraint<T, String> c) {
    c.predicates().add(ConstraintPredicate.of(x -> {
      try {
        new URI(x);
        return true;
      }
      catch (URISyntaxException e) {
        return false;
      }
    }, CHAR_SEQUENCE_URL, () -> new Object[] {}, VALID));
    return c;
  }

  public static <T> CharSequenceConstraint<T, String> isFile(CharSequenceConstraint<T, String> c) {
    c.predicates().add(ConstraintPredicate.of(x -> {
      try {
        var f = new File(x);
        return f.exists();
      } catch (Exception e) {
        return false;
      }
    }, ViolationMessage.of("file.exists", "\"{0}\" file does not exist"), () -> new Object[] {}, VALID));
    return c;
  }

  private static <T> boolean uniqueEntries(List<T> items, Function<T, String> keyOf) {
    var keys = new HashSet<String>();
    for (T it : items) {
      if (!keys.add(keyOf.apply(it))) {
        return false;
      }
    }
    return true;
  }

  private static boolean hasTlsCoverage(A4Server s) {
    if (s.tls != null && s.tls.base != null) { // matches fall back to domain cert/key
      return true;
    }
    for (var m : s.match) {
      if (m.tls == null) {
        return false;
      }
    }
    return true; // no domain cert/key fallback, but all matches define cert/key.
  }

  private static final Validator<A4HealthExec> A4HealthExecVld = ValidatorBuilder.<A4HealthExec>of()
    ._string(ex -> ex.command, "command", A4Valid::nnNeNb)
    .forEach(A4HealthExec::argList, "args", b -> b._string(s -> s, "arg", A4Valid::nnNeNb))
    .build();

  private static final Validator<A4HealthCheck> A4HealthCheckVld = ValidatorBuilder.<A4HealthCheck>of()
    ._integer(hc -> hc.intervalMs, "intervalMs", A4Valid::gtZero)
    ._integer(hc -> hc.timeoutMs, "timeoutMs", A4Valid::gtZero)
    .constraintOnCondition(
      (hc, cc) -> hc.intervalMs != null && hc.timeoutMs != null,
      b -> b.constraintOnTarget(
        hc -> hc.timeoutMs < hc.intervalMs, "timeoutMs",
        "timeoutMs.isLessThanInterval",
        "\"{0}\" \"timeoutMs\" must be less than \"intervalMs\""
      )
    )
    .nestIfPresent(hc -> hc.exec, "exec", A4HealthExecVld)
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
    )
    .nest(b -> b.addr, "addr", A4SockVld)
    .build();

  private static final Validator<A4StringOp> A4StringOpVld = ValidatorBuilder.<A4StringOp>of()
    .constraintOnCondition(
      (op, cg) -> op.equals != null,
      b -> b._string(op -> op.equals, A4MatchOps.OpEquals, A4Valid::nnNeNb)
    ).constraintOnCondition(
      (op, cg) -> op.startsWith != null,
      b -> b._string(op -> op.startsWith, A4MatchOps.OpStartsWith, A4Valid::nnNeNb)
    ).constraintOnCondition(
      (op, cg) -> op.endsWith != null,
      b -> b._string(op -> op.endsWith, A4MatchOps.OpEndsWith, A4Valid::nnNeNb)
    ).constraintOnTarget(
      op -> op.equals != null || op.endsWith != null || op.startsWith != null,
      anyOf, anyOf,
      "\"{0}\" missing any of " + Arrays.toString(A4MatchOps.Ops)
    ).constraintOnTarget(
      op -> allNoNullLtEq(1, op.equals, op.endsWith, op.startsWith),
      oneOf, oneOf,
      "\"{0}\" only one of " + Arrays.toString(A4MatchOps.Ops) + " allowed"
    ).build();

  private static final Validator<A4MatchOp> A4MatchOpVld = ValidatorBuilder.<A4MatchOp>of()
    .nestIfPresent(mo -> mo.host, "host", A4StringOpVld)
    .nestIfPresent(mo -> mo.sni, "sni", A4StringOpVld)
    .constraintOnTarget(
      mo -> mo.host != null || mo.sni != null,
      anyOf, anyOf,
      "\"{0}\" missing any of [host, sni]"
    ).build();

  private static final Validator<A4DiscHttp> A4DiscHttpVld = ValidatorBuilder.<A4DiscHttp>of()
    ._object(h -> h.format, "format", Constraint::notNull)
    ._string(h -> h.endpoint, "endpoint", c -> uri(nnNeNb(c)))
    .build();

  private static final Validator<A4DiscExec> A4DiscExecVld = ValidatorBuilder.<A4DiscExec>of()
    ._object(e -> e.format, "format", Constraint::notNull)
    ._string(e -> e.command, "command", A4Valid::nnNeNb)
    .forEach(A4DiscExec::argList, "args", b -> b._string(s -> s, "arg", A4Valid::nnNeNb))
    .build();

  private static final Validator<A4DiscK8s> A4DiscK8sVld = ValidatorBuilder.<A4DiscK8s>of()
    ._string(k -> k.apiUri, "apiUri", c -> uri(nnNeNb(c)))
    ._string(k -> k.tokenPath, "tokenPath", c -> isFile(nnNeNb(c)))
    ._string(k -> k.namespace, "namespace", A4Valid::nnNeNb)
    ._string(k -> k.service, "service", A4Valid::nnNeNb)
    ._integer(k -> k.port, "port", A4Valid::gtZero)
    .build();

  private static final Validator<A4Disc> A4DiscVld = ValidatorBuilder.<A4Disc>of()
    .nestIfPresent(d -> d.http, "http", A4DiscHttpVld)
    .nestIfPresent(d -> d.exec, "exec", A4DiscExecVld)
    .nestIfPresent(d -> d.k8s, "k8s", A4DiscK8sVld)
    ._integer(d -> d.intervalMs, "intervalMs", A4Valid::gtZero)
    ._integer(d -> d.timeoutMs, "timeoutMs", A4Valid::gtZero)
    .constraintOnCondition(
      (d, cc) -> d.intervalMs != null && d.timeoutMs != null,
      b -> b.constraintOnTarget(
        d -> d.timeoutMs < d.intervalMs, "timeoutMs",
        "timeoutMs.isLessThanInterval",
        "\"{0}\" \"timeoutMs\" must be less than \"intervalMs\""
      )
    )
    .constraintOnTarget(
      d -> d.http != null || d.exec != null || d.k8s != null,
      anyOf, anyOf,
      "\"{0}\" missing any of [http, exec, k8s]"
    ).constraintOnTarget(
      d -> allNoNullLtEq(1, d.http, d.exec, d.k8s),
      oneOf, oneOf,
      "\"{0}\" only one of [http, exec, k8s] allowed"
    )
    .build();

  private static final Validator<A4Pool> A4PoolVld = ValidatorBuilder.<A4Pool>of()
    .constraint(A4Pool::hostList, "hosts", Constraint::notNull)
    .forEach(A4Pool::hostList, "hosts", A4BackendVld)
    .build();

  private static boolean discoverIntervalGreaterThanHealthCheckInterval(A4Match m) {
    var test = m.discover != null
      && m.discover.intervalMs != null
      && m.healthCheck != null
      && m.healthCheck.intervalMs != null;
    return test;
  }

  private static boolean onlyOneOfDiscoverOrPoolHosts(A4Match m) {
    var test = m.discover != null
      && m.pool != null
      && m.pool.hosts != null
      && !m.pool.hosts.isEmpty();
    return !test;
  }

  private static boolean matchSniCanOnlyBeEquals(A4Match m) {
    if (m.op == null) {
      return true;
    }
    if (m.op.sni == null) {
      return true;
    }
    return m.op.sni.equals != null;
  }

  private static final Validator<A4Tls> A4TlsVld = ValidatorBuilder.<A4Tls>of()
    ._string(t -> t.certPath, "certPath", c -> isFile(nnNeNb(c)))
    ._string(t -> t.keyPath, "keyPath", c -> isFile(nnNeNb(c)))
    .build();

  private static final Validator<A4Match> A4MatchVld = ValidatorBuilder.<A4Match>of()
    .constraintOnCondition(
      (m, cg) -> discoverIntervalGreaterThanHealthCheckInterval(m),
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
    .constraintOnTarget(
      A4Valid::onlyOneOfDiscoverOrPoolHosts, oneOf, oneOf,
      "\"{0}\" only one of [discover, pool.hosts] allowed"
    )
    .constraintOnTarget(
      A4Valid::matchSniCanOnlyBeEquals,
      "match.op.sni", "op.sni", "\"{0}\" SNI match op can only be " + A4MatchOps.OpEquals
    )
    .nest(m -> m.pool, "pool", A4PoolVld)
    .nestIfPresent(m -> m.discover, "discover", A4DiscVld)
    .nestIfPresent(m -> m.healthCheck, "healthCheck", A4HealthCheckVld)
    .nestIfPresent(m -> m.tls, "tls", A4TlsVld)
    .nestIfPresent(m -> m.op, "op", A4MatchOpVld)
    .build();

  private static final Validator<A4ServerTls> A4ServerTlsVld = ValidatorBuilder.<A4ServerTls>of()
    ._object(A4ServerTls::protocolList, "protocols.notNull", Constraint::notNull)
    .forEach(A4ServerTls::protocolList, "protocols.version",
      svb -> svb._string(s -> s, "value", A4Valid::nnNeNb)
    )
    .forEach(A4ServerTls::cipherList, "ciphers.cipher",
      svb -> svb._string(s -> s, "value", A4Valid::nnNeNb)
    )
    .build();

  private static final Validator<A4Udp> A4UdpVld = ValidatorBuilder.<A4Udp>of()
    ._integer(u -> u.bufferSize, "bufferSize", A4Valid::gtZero)
    .build();

  private static final Validator<A4Server> A4ServerVld = ValidatorBuilder.<A4Server>of()
    ._string(s -> s.id, "id", A4Valid::nnNeNb)
    .nest(s -> s.addr, "addr", A4SockVld)
    .nestIfPresent(s -> s.tls, "tls", A4ServerTlsVld)
    .nestIfPresent(s -> s.udp, "udp", A4UdpVld)
    .constraint(A4Configs::allMatchesOf, "match", c -> c.notNull().notEmpty())
    .forEach(A4Configs::allMatchesOf, "match", A4MatchVld)
    .constraintOnTarget(
      s -> uniqueEntries(s.match, A4Match::matchLabel),
      "server.matchLabel", unique, "\"{0}\" must contain unique match labels"
    )
    .constraintOnTarget(
      s -> allNoNullLtEq(1, s.tls, s.udp),
      oneOf, oneOf,
      "\"{0}\" only one of [tls, udp] allowed"
    )
    .constraintOnCondition(
      (s, cg) -> s.udp != null,
      b ->
        b.forEach(
          A4Configs::allPoolsOf, "udpPool",
          pb -> pb.constraintOnTarget(
            p -> p.type != A4PoolType.leastConn, "poolType", "poolType",
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
    .constraintOnCondition(
      (s, cg) -> s.tls != null,
      b -> b.constraintOnTarget(
        A4Valid::hasTlsCoverage,
        "tls.coverage", "tls.coverage",
        "\"{0}\" must define a base certificate, or specify certificates for all SNI matches"
      )
    )
    .build();

  private static final Validator<A4Config> A4ConfigVld = ValidatorBuilder.<A4Config>of()
    ._string(c -> c.id, "id", A4Valid::nnNeNb)
    ._string(c -> c.description, "description", A4Valid::nnNeNb)
    .constraint(A4Config::serverList, "servers", c -> c.notNull().notEmpty())
    .forEach(A4Config::serverList, "servers", A4ServerVld)
    .constraintOnTarget(
      c -> uniqueEntries(c.servers, s -> s.id),
      "server.id", unique, "\"{0}\" must contain unique server ids"
    )
    .constraintOnTarget(
      c -> uniqueEntries(c.servers, s -> s.addr.id()),
      "server.addr.id", unique, "\"{0}\" must contain unique host/port definitions"
    )
    .build();

  private static final Validator<A4Options> A4FlagsVld = ValidatorBuilder.<A4Options>of()
    ._object(fl -> fl.root, A4Options.kConfig, Constraint::notNull)
    .constraintOnTarget(
      fl -> fl.root.exists(), A4Options.kConfig, "exists",
      "\"{0}\" does not exist (not a file or directory)"
    )
    .nest(fl -> fl.api, "--api-*", A4SockVld)
    .build();

  private static final Map<Class<?>, Validator<?>> validators = new HashMap<>();

  static {
    validators.put(A4Backend.class, A4BackendVld);
    validators.put(A4Config.class, A4ConfigVld);
    validators.put(A4Options.class, A4FlagsVld);
    validators.put(A4DiscK8s.class, A4DiscK8sVld);
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

  public static Collection<A4Validation> validationsOf(ConstraintViolations cv) {
    return cv.stream().map(c -> {
      var v = new A4Validation();
      v.name = c.name();
      v.args = new String[c.args().length];
      v.message = c.message();
      v.key = c.messageKey();
      v.format = c.defaultMessageFormat();
      for (int i = 0; i < c.args().length; i++) {
        v.args[i] = c.args()[i] != null ? c.args()[i].toString() : "";
      }
      return v;
    }).collect(Collectors.toList());
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
