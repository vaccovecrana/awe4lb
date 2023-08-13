package io.vacco.a4lb;

import org.slf4j.*;
import java.io.IOException;
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

  public static String stateOf(Socket s) {
    return String.format("%s (bnd: %s, con: %s, cls: %s)",
        s.toString(), s.isBound(), s.isConnected(), s.isClosed()
    );
  }

  public static long io(String cid, Socket in, Socket out) {
    try {
      var is = in.getInputStream();
      var os = out.getOutputStream();
      if (log.isTraceEnabled()) {
        log.trace("{} - I/O start", cid);
      }
      var bytes = is.transferTo(os);
      if (log.isTraceEnabled()) {
        log.trace("{} - {} bytes transferred", cid, bytes);
      }
      return bytes;
    } catch (Exception e) {
      log.error(
          "{} - Socket I/O error - {} {}",
          cid, stateOf(in), stateOf(out), e
      );
      return -1;
    }
  }

}
