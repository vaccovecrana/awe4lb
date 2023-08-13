package io.vacco.a4lb;

import org.slf4j.*;
import stormpot.*;
import java.io.IOException;
import java.net.Socket;

public class A4PTcpExp implements Expiration<A4PTcp> {

  private static final Logger log = LoggerFactory.getLogger(A4PTcpExp.class);

  @Override public boolean hasExpired(SlotInfo<? extends A4PTcp> info) {

    info.getPoolable().expire();

    var sock = info.getPoolable().object;
    /*
     * TODO not ideal, but opening a temporary new socket should be a good indication that the backend is still up.
     */
    try (var socket = new Socket()) {
      socket.connect(sock.getRemoteSocketAddress());
      return false;
    } catch (IOException e) {
      if (log.isTraceEnabled()) {
        log.trace("{} - socket invalidated", sock);
      }
      return true;
    }
  }
}
