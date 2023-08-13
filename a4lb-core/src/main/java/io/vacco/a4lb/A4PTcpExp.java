package io.vacco.a4lb;

import org.slf4j.*;
import stormpot.*;
import java.io.IOException;

public class A4PTcpExp implements Expiration<A4PTcp> {

  private static final Logger log = LoggerFactory.getLogger(A4PTcpExp.class);

  @Override public boolean hasExpired(SlotInfo<? extends A4PTcp> info) {
    var sock = info.getPoolable().object;
    try {
      sock.sendUrgentData(0);
      return false;
    } catch (IOException e) {
      if (log.isTraceEnabled()) {
        log.trace("{} - socket invalidated", sock);
      }
      return true;
    }
  }
}
