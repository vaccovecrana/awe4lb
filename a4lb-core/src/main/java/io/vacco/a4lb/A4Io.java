package io.vacco.a4lb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;

public class A4Io {

  public static final Logger log = LoggerFactory.getLogger(A4Io.class);

  public static int eofRead(SelectionKey k, SocketChannel sc, ByteBuffer bb) {
    try {
      bb.clear();
      int bytesRead = sc.read(bb);
      if (bytesRead == -1) {
        if (log.isTraceEnabled()) {
          // TODO find a way to release the backend socket channel back to the pool.
          log.trace("{} - socket channel EOF", sc.socket());
        }
        close(sc);
        k.cancel();
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

  public static Selector osSelector() {
    try {
      return Selector.open();
    } catch (IOException ioe) {
      log.error("Unable to obtain OS selector", ioe);
      throw new IllegalStateException(ioe);
    }
  }

  public static ServerSocketChannel openServer(Selector sel, InetSocketAddress target) {
    // TODO
    //   this needs to accept custom configurations for each ingress type (tcp vs tls)
    //   also what about UDP server socket channels? :/
    try {
      var ssc = ServerSocketChannel.open();
      ssc.bind(target);
      ssc.configureBlocking(false);
      ssc.register(sel, SelectionKey.OP_ACCEPT);
      log.info("{} - Ingress open", ssc.socket());
      return ssc;
    } catch (IOException ioe) {
      log.error("Unable to open server socket channel {}", target, ioe);
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

  public static void zeroFill(ByteBuffer bb) { // TODO there's likely a faster way to do this.
    bb.clear();
    int len = bb.capacity() - 1;
    for (int i = 0; i < len; i++) {
      bb.put(i, (byte) 0x00);
    }
  }

}
