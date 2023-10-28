package io.vacco.a4lb.util;

import io.vacco.a4lb.cfg.*;
import java.util.List;
import java.util.Optional;

public class A4MatchOps {

  public static boolean eval(A4StringOp op, String val) {
    if (val == null) {
      return false;
    }
    val = val.toUpperCase();
    if (op.equals != null) {
      return op.equals.toUpperCase().equals(val);
    }
    if (op.contains != null) {
      return val.contains(op.contains.toUpperCase());
    }
    if (op.startsWith != null) {
      return val.startsWith(op.startsWith.toUpperCase());
    }
    if (op.endsWith != null) {
      return val.endsWith(op.endsWith.toUpperCase());
    }
    return false;
  }

  public static boolean eval(A4MatchOp op, String sni, String host) {
    if (op.sni != null) {
      return eval(op.sni, sni);
    }
    return eval(op.host, host);
  }

  public static boolean evalAnd(String sni, String host, A4MatchOp ... ops) {
    for (var op : ops) {
      var out = eval(op, sni, host);
      if (!out) {
        return false;
      }
    }
    return true;
  }

  public static boolean evalOr(String sni, String host, A4MatchOp ... ops) {
    for (var op : ops) {
      if (eval(op, sni, host)) {
        return true;
      }
    }
    return false;
  }

  public static Optional<A4Pool> eval(String sni, String host, List<A4Match> rules) {
    for (var rule : rules) {
      var out = rule.and != null
          ? evalAnd(sni, host, rule.and)
          : rule.or == null || evalOr(sni, host, rule.or);
      if (out) {
        return Optional.of(rule.pool);
      }
    }
    return Optional.empty();
  }

}
