package io.vacco.a4lb.sel;

import io.vacco.a4lb.cfg.A4Backend;
import io.vacco.a4lb.cfg.A4Pool;

public class A4SelStd {

  public static A4Backend select(A4Pool pool, A4PoolState poolState) {
    var up = pool.upHosts();
    if (up.size() == 1) {
      return up.get(0);
    }
    int idx = poolState.rnd.nextInt(up.size());
    return up.get(idx);
  }

}
