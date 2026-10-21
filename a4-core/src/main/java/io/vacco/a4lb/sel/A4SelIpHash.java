package io.vacco.a4lb.sel;

import io.vacco.a4lb.cfg.A4Backend;
import io.vacco.a4lb.cfg.A4Pool;

public class A4SelIpHash {

  private static long getUnsignedInt(int x) {
    return x & 0x00000000ffffffffL;
  }

  public static A4Backend select(A4Pool pool, int clientIpHash) {
    var up = pool.upHosts();
    if (up.isEmpty()) {
      throw new IllegalStateException("No available backends for IP-hash selection");
    }
    var ipl = getUnsignedInt(clientIpHash);
    var idx = ipl % up.size();
    return up.get((int) idx);
  }

}
