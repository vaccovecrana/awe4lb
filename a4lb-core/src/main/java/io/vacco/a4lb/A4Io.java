package io.vacco.a4lb;

import org.slf4j.*;
import java.net.Socket;

public class A4Io {

  private static final Logger log = LoggerFactory.getLogger(A4Io.class);

  public static void close(Socket s) {
    try {
      s.close();
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.error("Unable to close socket {}", s, e);
      }
    }
  }

  public static String connId(Socket s0, Socket s1) {
    return String.format("[%s:%d -> %s:%d]",
        s0.getInetAddress(), s0.getPort(),
        s1.getInetAddress(), s1.getPort()
    );
  }

}
