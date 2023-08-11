package io.vacco.a4lb;

import org.slf4j.*;
import java.io.IOException;
import java.net.Socket;
import java.util.function.BiConsumer;

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

  public static void io(Socket in, Socket out, BiConsumer<String, Long> ioCons) {
    var cid = A4Io.connId(in, out);
    try {
      var is = in.getInputStream();
      var os = out.getOutputStream();
      if (log.isTraceEnabled()) {
        log.trace("{} - I/O start", cid);
      }
      ioCons.accept(cid, is.transferTo(os));
    } catch (IOException e) {
      log.error("{} - Socket I/O error", cid, e);
      ioCons.accept(cid, -1L);
    }
  }

  public static boolean isSocketUsable(Socket s) {
    try {
      s.sendUrgentData(0);
      return true;
    } catch (IOException e) {
      return false;
    }
  }

}
