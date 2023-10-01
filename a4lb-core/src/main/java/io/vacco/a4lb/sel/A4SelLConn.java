package io.vacco.a4lb.sel;

import io.vacco.a4lb.cfg.A4Backend;
import io.vacco.a4lb.cfg.A4Pool;
import java.util.Comparator;

public class A4SelLConn {

  public static A4Backend select(A4Pool pool, A4Selector bkSel) {
    var up = pool.upHosts();
    return up
        .stream()
        .min(Comparator.comparingInt(bkSel::connCountOf))
        .orElseThrow();
  }

}