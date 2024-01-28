package io.vacco.a4lb;

import io.vacco.a4lb.service.A4Context;
import io.vacco.a4lb.util.*;

public class A4LbMain {

  public static void main(String[] args) throws InterruptedException {
    if (args == null || args.length == 0) {
      System.out.println(A4Options.usage());
      return;
    }
    var ctx = new A4Context();
    try { // TODO add UNIX SIGTERM handler
      ctx.init(A4Options.from(args));
    } catch (Exception e) {
      System.out.printf("Unable to initialize load balancer - %s %s%n",
        e.getClass().getSimpleName(), e.getMessage()
      );
      ctx.close();
    }
  }

}
