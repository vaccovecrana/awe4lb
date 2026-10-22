package io.vacco.a4lb.sel;

import io.vacco.a4lb.cfg.A4Backend;
import io.vacco.a4lb.cfg.A4Pool;

import java.util.List;
import java.util.Random;
import java.util.TreeMap;

import static java.util.stream.Collectors.groupingBy;

public class A4SelWeight {

  private static A4Backend wtRdSelect(List<A4Backend> backends, Random rnd) {
    if (backends == null || backends.isEmpty()) {
      throw new IllegalStateException("No available backends for weighted selection");
    }
    int tw = 0;
    for (var b : backends) {
      tw = tw + weightOf(b);
    }
    if (tw <= 0) {
      throw new IllegalStateException("Weighted selection requires at least one positive backend weight");
    }
    int rw = rnd.nextInt(tw);
    int cw = 0;
    for (var backend : backends) {
      cw += weightOf(backend);
      if (rw < cw) {
        return backend;
      }
    }
    var msg = String.format("Invalid host weight state: %s, %d, %d", backends, rw, cw);
    throw new IllegalStateException(msg);
  }

  private static int weightOf(A4Backend b) {
    return b.weight != null && b.weight > 0 ? b.weight : 1;
  }

  public static A4Backend select(A4Pool pool, A4PoolContext poolCtx) {
    var up = pool.upHosts();
    if (up.isEmpty()) {
      throw new IllegalStateException("No available backends for weighted selection");
    }
    var hostIdx = new TreeMap<>(up.stream().collect(groupingBy(bk -> bk.priority != null ? bk.priority : 0)));
    return wtRdSelect(hostIdx.values().iterator().next(), poolCtx.rnd);
  }

}
