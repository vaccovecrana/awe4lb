package io.vacco.a4lb;

import org.slf4j.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.function.Consumer;

public class A4Io {

  public static final Logger log = LoggerFactory.getLogger(A4Io.class);

  public static void close(SocketChannel sc) {
    try {
      if (sc != null) {
        sc.close();
      }
    } catch (IOException ioe) {
      if (log.isTraceEnabled()) {
        log.trace("Unable to close socket channel {}", sc.socket(), ioe);
      }
    }
  }

  public static int eofRead(SocketChannel sc, ByteBuffer bb) {
    try {
      bb.clear();
      int bytesRead = sc.read(bb);
      if (bytesRead == -1) {
        if (log.isTraceEnabled()) {
          log.trace("{} - socket channel EOF", sc.socket());
        }
      } else if (bytesRead > 0) {
        bb.flip();
      }
      return bytesRead;
    } catch (IOException ioe) {
      if (log.isTraceEnabled()) {
        log.trace("Unable to read data from socket channel {}", sc.socket(), ioe);
      }
      throw new IllegalStateException(ioe);
    }
  }

  public static Selector osSelector() {
    try {
      return Selector.open();
    } catch (IOException ioe) {
      log.error("Unable to obtain OS selector", ioe);
      throw new IllegalStateException(ioe);
    }
  }

  public static void select(Selector sel, Consumer<SelectionKey> onKey) {
    try {
      sel.select();
      var kit = sel.selectedKeys().iterator();
      while (kit.hasNext()) {
        var key = kit.next();
        kit.remove();
        try {
          onKey.accept(key);
        } catch (Exception e) {
          log.error("Unhandled key processing error. Discarding. - {}", key, e);
          key.cancel();
        }
      }
    } catch (IOException ioe) {
      log.error("Unable to perform selection - {}", sel, ioe);
    }
  }

}
