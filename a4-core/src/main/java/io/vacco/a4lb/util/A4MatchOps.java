package io.vacco.a4lb.util;

import io.vacco.a4lb.cfg.*;
import java.util.List;
import java.util.Optional;

public class A4MatchOps {

  public static final String OpEquals = "equals";
  public static final String OpEndsWith = "endsWith";
  public static final String OpStartsWith = "startsWith";
  public static final String[] Ops = new String[] { OpEquals, OpEndsWith, OpStartsWith };

  public static boolean eval(A4StringOp op, String val) {
    if (val == null) {
      return false;
    }
    val = val.toUpperCase();
    if (op.equals != null) {
      return op.equals.toUpperCase().equals(val);
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
    if (op.sni != null && op.host != null) {
      return eval(op.sni, sni) && eval(op.host, host);
    }
    if (op.sni != null) {
      return eval(op.sni, sni);
    }
    if (op.host != null) {
      return eval(op.host, host);
    }
    return false;
  }

  public static Optional<A4Match> eval(String sni, String host, List<A4Match> rules) {
    for (var rule : rules) {
      if (rule.op != null) {
        if (eval(rule.op, sni, host)) {
          return Optional.of(rule);
        }
      } else {
        return Optional.of(rule);
      }
    }
    return Optional.empty();
  }

}
