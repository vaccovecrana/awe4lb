package io.vacco.a4lb.tcp;

import io.vacco.a4lb.cfg.A4Backend;
import io.vacco.a4lb.cfg.A4Pool;

public class A4SelRRobin {

  public static A4Backend select(A4Pool pool) {
    pool.rrIdx = pool.rrIdx + 1;
    if (pool.rrIdx == Integer.MAX_VALUE) {
      pool.rrIdx = 0;
    }
    var up = pool.upHosts();
    return up.get(pool.rrIdx % (up.size() - 1));
  }

}
