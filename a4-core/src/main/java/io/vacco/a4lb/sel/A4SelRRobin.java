package io.vacco.a4lb.sel;

import io.vacco.a4lb.cfg.A4Backend;
import io.vacco.a4lb.cfg.A4Pool;

public class A4SelRRobin {

  public static A4Backend select(A4Pool pool, A4PoolContext poolCtx) {
    poolCtx.rrUpdate();
    var up = pool.upHosts();
    var idx = poolCtx.rrVal % up.size();
    return up.get(idx);
  }

}
