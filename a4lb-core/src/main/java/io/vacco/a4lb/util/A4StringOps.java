package io.vacco.a4lb.util;

import io.vacco.a4lb.cfg.A4StringOp;

public class A4StringOps {

  public boolean eval(A4StringOp op, String val) {
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

}
