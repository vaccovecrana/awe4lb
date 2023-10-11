package io.vacco.a4lb;

import io.vacco.a4lb.tcp.A4Io;
import io.vacco.a4lb.util.*;

public class A4LbMain {

  private static void stop(A4Service svc) {
    if (svc != null) {
      A4Io.close(svc);
    }
  }

  public static void main(String[] args) {
    if (args == null || args.length == 0) {
      System.out.println(A4Flags.usage());
      return;
    }
    var svc = new A4Service();
    try {
      svc.init(A4Flags.from(args));
    } catch (Exception e) { // TODO add UNIX signal handler
      stop(svc);
      System.exit(-1);
    }
    stop(svc);
  }

}
