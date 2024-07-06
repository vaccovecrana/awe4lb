package io.vacco.a4lb.sel;

import io.vacco.a4lb.cfg.*;
import java.util.*;

import static java.util.stream.Collectors.groupingBy;

public class A4SelWeight {

  public static A4Backend wtRdSelect(List<A4Backend> backends, Random rnd) {
    int tw = 0;
    for (var b : backends) {
      tw = tw + b.weight;
    }
    int rw = rnd.nextInt(tw);
    int cw = 0;
    for (var backend : backends) {
      cw += backend.weight;
      if (rw < cw) {
        return backend;
      }
    }
    var msg = String.format("Invalid host weight state: %s, %d, %d", backends, rw, cw);
    throw new IllegalStateException(msg);
  }

  public static A4Backend select(A4Pool pool, A4PoolContext poolCtx) {
    var up = pool.upHosts();
    var hostIdx = new TreeMap<>(up.stream().collect(groupingBy(bk -> bk.priority)));
    return wtRdSelect(hostIdx.values().iterator().next(), poolCtx.rnd);
  }

}
