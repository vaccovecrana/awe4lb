package io.vacco.a4lb;

import io.vacco.a4lb.service.A4Context;
import io.vacco.a4lb.util.A4Options;

import java.util.Arrays;

public class A4LbMain {

  public static void main(String[] args) {
    if (args == null || args.length == 0 || Arrays.asList(args).contains("--help")) {
      System.out.println(A4Options.usage());
      return;
    }
    var ctx = new A4Context();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        ctx.close();
      } catch (Exception e) {
        System.err.printf("Unable to stop load balancer - %s %s%n",
          e.getClass().getSimpleName(), e.getMessage()
        );
      }
    }, "a4lb-shutdown"));
    try {
      ctx.init(A4Options.from(args));
    } catch (Exception e) {
      System.out.printf("Unable to initialize load balancer - %s %s%n",
        e.getClass().getSimpleName(), e.getMessage()
      );
      ctx.close();
    }
  }

}
