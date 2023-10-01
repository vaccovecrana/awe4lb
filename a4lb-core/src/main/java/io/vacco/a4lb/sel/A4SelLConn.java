package io.vacco.a4lb.sel;

import io.vacco.a4lb.cfg.A4Backend;
import io.vacco.a4lb.cfg.A4Pool;
import java.util.Comparator;
import java.util.List;

public class A4SelLConn {

  public static A4Backend select(List<A4Backend> upHosts) {
    return upHosts
        .stream()
        .min(Comparator.comparingInt(bk -> bk.connections))
        .orElseThrow();
  }

}