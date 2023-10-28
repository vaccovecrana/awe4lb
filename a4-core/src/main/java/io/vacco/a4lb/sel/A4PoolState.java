package io.vacco.a4lb.sel;

import io.vacco.a4lb.cfg.A4Config;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public class A4PoolState {

  public ReentrantLock  lock = new ReentrantLock();
  public Random         rnd = new Random(A4Config.Seed);
  public int            rrVal = 0;

  public void rrUpdate() {
    rrVal = rrVal + 1;
    if (rrVal == Integer.MAX_VALUE) {
      rrVal = 0;
    }
  }

}
