package io.vacco.a4lb.sel;

import io.vacco.a4lb.cfg.A4Backend;
import io.vacco.a4lb.cfg.A4Pool;

public class A4SelIpHash {

  public static long getUnsignedInt(int x) {
    return x & 0x00000000ffffffffL;
  }

  public static A4Backend select(A4Pool pool, int clientIpHash) {
    var ipl = getUnsignedInt(clientIpHash);
    var up = pool.upHosts();
    var idx = ipl % (up.size() - 1);
    return up.get((int) idx);
  }

}