package io.vacco.a4lb.tcp;

import io.vacco.a4lb.cfg.A4Backend;
import io.vacco.a4lb.cfg.A4Pool;
import java.util.Comparator;

public class A4SelLConn {

  public static A4Backend select(A4Pool pool) {
    return pool.upHosts()
        .stream()
        .min(Comparator.comparingInt(bk -> bk.connections))
        .get();
  }

}