package io.vacco.a4lb.util;

import am.ik.yavi.core.ConstraintViolation;
import am.ik.yavi.core.ConstraintViolations;
import io.vacco.a4lb.cfg.A4Match;
import java.util.List;
import java.util.stream.Collectors;

public class A4Exceptions {

  public static final long ver = 1;

  public static class A4ValidationException extends RuntimeException {
    private static final long serialVersionUID = ver;
    public transient final List<String> violations;
    public A4ValidationException(ConstraintViolations violations) {
      super("invalid configuration");
      this.violations = violations.stream()
          .map(ConstraintViolation::message)
          .collect(Collectors.toUnmodifiableList());
    }
    @Override public String toString() {
      return violations.toString();
    }
  }

  public static class A4SelectException extends RuntimeException {
    private static final long serialVersionUID = ver;
    public final String clientHost;
    public final String tlsSni;
    public transient final List<A4Match> cfg;
    public A4SelectException(String clientHost, String tlsSni, List<A4Match> cfg, Exception cause) {
      super(String.format("Backend selection error [host: %s, sni: %s]", clientHost, tlsSni), cause);
      this.clientHost = clientHost;
      this.tlsSni = tlsSni;
      this.cfg = cfg;
    }
  }

  public static Throwable rootCauseOf(Throwable t){
    var root = t;
    while (root.getCause() != null && root.getCause() != root) {
      root = root.getCause();
    }
    return root;
  }

  public static String messageFor(Throwable t) {
    var x = A4Exceptions.rootCauseOf(t);
    return x.getMessage() != null && !x.getMessage().isEmpty()
        ? x.getMessage()
        : x.getClass().getSimpleName();
  }

}
