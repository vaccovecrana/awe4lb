package io.vacco.a4lb.util;

import org.slf4j.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class A4Io {

  public static final Logger log = LoggerFactory.getLogger(A4Io.class);

  public static void close(Closeable c) {
    try {
      if (c != null) {
        c.close();
      }
    } catch (IOException e) {
      if (log.isDebugEnabled()) {
        log.debug("Unable to close {}", c, e);
      }
    }
  }

  public static int eofRead(ByteChannel c, ByteBuffer bb) {
    try {
      bb.clear();
      int bytesRead = c.read(bb);
      if (bytesRead == -1) {
        if (log.isTraceEnabled()) {
          log.trace("{} - channel EOF", c);
        }
      } else if (bytesRead > 0) {
        bb.flip();
      }
      return bytesRead;
    } catch (IOException ioe) {
      var msg = String.format("Unable to read data from channel %s", c);
      throw new IllegalStateException(msg, ioe);
    }
  }

  public static int eofWrite(ByteChannel c, ByteBuffer bb) {
    try {
      return c.write(bb);
    } catch (IOException ioe) {
      var msg = String.format("Unable to write data to channel %s", c);
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
          log.error("Unhandled key processing error. Closing. - {}", key, e);
          key.cancel();
        }
      }
    } catch (IOException ioe) {
      log.error("Unable to perform selection - {}", sel, ioe);
    }
  }

  public static InputStream openStream(URI uri, int timeoutMs) throws IOException {
    var conn = uri.toURL().openConnection();
    conn.setConnectTimeout(timeoutMs);
    conn.setReadTimeout(timeoutMs);
    return conn.getInputStream();
  }

  public static String loadContent(URI uri, int timeoutMs) {
    try (var in = openStream(uri, timeoutMs)) {
      var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
      return reader.lines().collect(Collectors.joining(System.lineSeparator()));
    } catch (Exception e) {
      throw new IllegalStateException("Unable to load content from " + uri, e);
    }
  }

  public static String loadContent(URI uri, HttpClient client, String bearerToken, int timeoutMs) {
    try {
      var request = HttpRequest.newBuilder()
        .GET().uri(uri)
        .header("Authorization", "Bearer " + bearerToken)
        .timeout(Duration.of(timeoutMs, ChronoUnit.MILLIS))
        .build();
      var response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new RuntimeException("Failed : HTTP error code : " + response.statusCode());
      }
      return response.body();
    } catch (Exception e) {
      throw new IllegalStateException("Unable to load content from " + uri, e);
    }
  }

}
