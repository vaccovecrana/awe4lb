package io.vacco.a4lb.sel;

import io.vacco.a4lb.cfg.A4Backend;
import io.vacco.a4lb.cfg.A4Pool;

import java.util.Comparator;
import java.util.Map;

public class A4SelLConn {

  public static A4Backend select(A4Pool pool, Map<A4Backend, A4BackendContext> bkContextIdx) {
    var up = pool.upHosts();
    if (up.isEmpty()) {
      throw new IllegalStateException("No available backends for least-connections selection");
    }
    return up
        .stream()
        .min(Comparator.comparingInt(bk ->
            bkContextIdx.computeIfAbsent(bk, b -> new A4BackendContext()).activeConnections()))
        .orElseThrow();
  }

}
