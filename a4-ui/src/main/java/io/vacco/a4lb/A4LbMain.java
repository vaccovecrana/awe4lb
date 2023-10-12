package io.vacco.a4lb;

import io.vacco.a4lb.util.A4Io;
import io.vacco.a4lb.util.*;

public class A4LbMain {

  public static void main(String[] args) {
    if (args == null || args.length == 0) {
      System.out.println(A4Flags.usage());
      return;
    }
    var svc = new A4Service();
    try {
      svc.init(A4Flags.from(args));
    } catch (Exception e) { // TODO add UNIX signal handler
      A4Io.close(svc);
      System.exit(-1);
    }
    A4Io.close(svc);
  }

}
