package io.vacco.a4lb.sel;

import io.vacco.a4lb.cfg.A4Backend;
import io.vacco.a4lb.cfg.A4Pool;

public class A4SelRRobin {

  public static A4Backend select(A4Pool pool) {
    pool.rrVal = pool.rrVal + 1;
    if (pool.rrVal == Integer.MAX_VALUE) {
      pool.rrVal = 0;
    }
    var up = pool.upHosts();
    return up.get(pool.rrVal % (up.size() - 1));
  }

}
