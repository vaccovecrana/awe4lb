package io.vacco.a4lb.sel;

import io.vacco.a4lb.cfg.A4Config;
import java.util.Random;

public class A4PoolContext {

  public Random rnd = new Random(A4Config.Seed);
  public int    rrVal = 0;

  public void rrUpdate() {
    rrVal = rrVal + 1;
    if (rrVal == Integer.MAX_VALUE) {
      rrVal = 0;
    }
  }

}
