package io.vacco.a4lb.tcp;

import io.vacco.a4lb.cfg.A4Backend;
import io.vacco.a4lb.util.A4Exceptions;
import org.slf4j.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class A4Io {

  public static final Logger log = LoggerFactory.getLogger(A4Io.class);

  public static void close(SocketChannel sc) {
    try {
      if (sc != null) {
        sc.close();
      }
    } catch (IOException ioe) {
      if (log.isDebugEnabled()) {
        log.debug("Unable to close socket channel {}", sc.socket(), ioe);
      }
    }
  }

  public static int eofRead(String channelId, ByteChannel sc, ByteBuffer bb) {
    try {
      bb.clear();
      int bytesRead = sc.read(bb);
      if (bytesRead == -1) {
        if (log.isDebugEnabled()) {
          log.debug("{} - channel EOF", channelId);
        }
      } else if (bytesRead > 0) {
        bb.flip();
      }
      return bytesRead;
    } catch (IOException ioe) {
      var msg = String.format("Unable to read data from channel %s", channelId);
      throw new IllegalStateException(msg, ioe);
    }
  }

  public static Selector newSelector() {
    try {
      return Selector.open();
    } catch (IOException ioe) {
      throw new IllegalStateException("Unable to obtain OS selector", ioe);
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
          if (key.isValid()) {
            onKey.accept(key);
          } else if (log.isTraceEnabled()) {
            log.trace("{} - Invalid key state", key);
          }
        } catch (Exception e) {
          log.error("Unhandled key processing error. Discarding. - {}", key, e);
          key.cancel();
        }
      }
    } catch (IOException ioe) {
      log.error("Unable to perform selection - {}", sel, ioe);
    }
  }

  public static A4Backend.State stateOf(A4Backend bk, int timeOutMs) {
    try (var socket = new Socket()) {
      socket.connect(new InetSocketAddress(bk.addr.host, bk.addr.port), timeOutMs);
      return A4Backend.State.Up;
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        var x = A4Exceptions.rootCauseOf(e);
        log.debug("{} - TCP health check failed - {} - {}", bk, x.getClass().getSimpleName(), x.getMessage());
      }
      return A4Backend.State.Down;
    }
  }

  public static String loadContent(URL u) {
    try (var in = u.openStream()) {
      var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
      return reader.lines().collect(Collectors.joining(System.lineSeparator()));
    } catch (Exception e) {
      throw new IllegalStateException("Unable to load content from " + u, e);
    }
  }

}
