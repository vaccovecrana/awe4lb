package io.vacco.a4lb.util;

import am.ik.yavi.core.ConstraintViolations;
import io.vacco.a4lb.cfg.A4Match;

public class A4Exceptions {

  public static class A4ConfigException extends RuntimeException {
    public final ConstraintViolations violations;
    public A4ConfigException(ConstraintViolations violations) {
      this.violations = violations;
    }
  }

  public static class A4SelectException extends RuntimeException {
    public final String clientHost;
    public final String tlsSni;
    public final A4Match[] cfg;
    public A4SelectException(String clientHost, String tlsSni, A4Match[] cfg, Exception cause) {
      super(String.format("Backend selection error [host: %s, sni: %s]", clientHost, tlsSni), cause);
      this.clientHost = clientHost;
      this.tlsSni = tlsSni;
      this.cfg = cfg;
    }
  }

}