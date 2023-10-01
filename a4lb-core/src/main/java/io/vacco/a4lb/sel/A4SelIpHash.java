package io.vacco.a4lb.sel;

import io.vacco.a4lb.cfg.A4Backend;
import io.vacco.a4lb.cfg.A4Pool;

import java.util.List;

public class A4SelIpHash {

  public static long getUnsignedInt(int x) {
    return x & 0x00000000ffffffffL;
  }

  public static A4Backend select(List<A4Backend> upHosts, int clientIpHash) {
    var ipl = getUnsignedInt(clientIpHash);
    var idx = ipl % (upHosts.size() - 1);
    return upHosts.get((int) idx);
  }

}