package io.vacco.a4lb.tcp;

import org.slf4j.*;
import java.io.*;
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

  public static InputStream openStream(URL url, int timeoutMs) throws IOException {
    var conn = url.openConnection();
    conn.setConnectTimeout(timeoutMs);
    conn.setReadTimeout(timeoutMs);
    return conn.getInputStream();
  }

  public static String loadContent(URL u, int timeoutMs) {
    try (var in = openStream(u, timeoutMs)) {
      var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
      return reader.lines().collect(Collectors.joining(System.lineSeparator()));
    } catch (Exception e) {
      throw new IllegalStateException("Unable to load content from " + u, e);
    }
  }

}
