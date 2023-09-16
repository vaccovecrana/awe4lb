package io.vacco.a4lb.tcp;

import io.vacco.a4lb.cfg.A4Backend;
import io.vacco.a4lb.cfg.A4Pool;

public class A4SelStd {

  public static A4Backend select(A4Pool pool) {
    var up = pool.upHosts();
    if (up.size() == 1) {
      return up.get(0);
    }
    return up.get(pool.rnd.nextInt(up.size()));
  }

}
