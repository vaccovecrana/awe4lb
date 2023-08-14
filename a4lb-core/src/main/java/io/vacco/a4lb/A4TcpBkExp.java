package io.vacco.a4lb;

import org.slf4j.*;
import stormpot.*;

public class A4TcpBkExp implements Expiration<A4TcpBk> {

  private static final Logger log = LoggerFactory.getLogger(A4TcpBkExp.class);

  @Override public boolean hasExpired(SlotInfo<? extends A4TcpBk> info) {
    var conn = info.getPoolable().channel.isConnected();
    if (log.isTraceEnabled()) {
      log.trace("{} - conn? {}", info.getPoolable().id, conn);
    }
    return !conn;
  }
}
