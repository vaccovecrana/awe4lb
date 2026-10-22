package io.vacco.a4lb.sel;

import io.vacco.a4lb.cfg.A4Backend;
import io.vacco.a4lb.cfg.A4Pool;

public class A4SelRandom {

  public static A4Backend select(A4Pool pool, A4PoolContext poolCtx) {
    var up = pool.upHosts();
    if (up.isEmpty()) {
      throw new IllegalStateException("No available backends for random selection");
    }
    if (up.size() == 1) {
      return up.get(0);
    }
    int idx = poolCtx.rnd.nextInt(up.size());
    return up.get(idx);
  }

}
