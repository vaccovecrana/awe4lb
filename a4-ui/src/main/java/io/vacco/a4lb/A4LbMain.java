package io.vacco.a4lb;

import io.vacco.a4lb.service.A4Context;
import io.vacco.a4lb.util.A4Io;
import io.vacco.a4lb.util.*;

public class A4LbMain {

  public static void main(String[] args) {
    if (args == null || args.length == 0) {
      System.out.println(A4Flags.usage());
      return;
    }
    var ctx = new A4Context();
    try {
      ctx.init(A4Flags.from(args));
    } catch (Exception e) { // TODO add UNIX SIGTERM handler
      A4Io.close(ctx);
    }
    A4Io.close(ctx);
  }

}
