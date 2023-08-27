package io.vacco.a4lb.util;

import am.ik.yavi.core.ConstraintViolations;

public class A4Exceptions {

  public static class A4ConfigException extends RuntimeException {
    public final ConstraintViolations violations;
    public A4ConfigException(ConstraintViolations violations) {
      this.violations = violations;
    }
  }

}
