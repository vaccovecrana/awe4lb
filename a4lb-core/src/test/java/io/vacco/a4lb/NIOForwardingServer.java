package io.vacco.a4lb;

import io.vacco.shax.logging.ShOption;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class NIOForwardingServer {

  static {
    ShOption.setSysProp(ShOption.IO_VACCO_SHAX_DEVMODE, "true");
    ShOption.setSysProp(ShOption.IO_VACCO_SHAX_PRETTYPRINT, "true");
    ShOption.setSysProp(ShOption.IO_VACCO_SHAX_LOGLEVEL, "trace");
  }

  public static SocketChannel openBackend(Selector sel) throws IOException {
    var bkc = SocketChannel.open();
    bkc.connect(new InetSocketAddress("localhost", 6900));
    bkc.configureBlocking(false);
    bkc.register(sel, SelectionKey.OP_READ);
    return bkc;
  }

  public static void main(String[] args) {

    var selector = A4Io.osSelector();
    var buffer = ByteBuffer.allocate(16);

    final SocketChannel[] client = { null };
    final SocketChannel[] backend = { null };
    A4Io.openServer(selector, new InetSocketAddress(8080));

    while (true) {
      A4Io.select(selector, key -> {
        try {
          if (key.isAcceptable()) {
            var serverChannel = (ServerSocketChannel) key.channel();
            client[0] = serverChannel.accept();
            client[0].configureBlocking(false);
            client[0].register(selector, SelectionKey.OP_READ);
            if (backend[0] == null) {
              backend[0] = openBackend(selector);
            }
          } else if (key.isReadable()) {
            if (key.channel() == client[0]) {
              if (A4Io.eofRead(key, client[0], buffer) == -1) {
                return;
              }
            } else if (A4Io.eofRead(key, backend[0], buffer) == -1) {
              return;
            }
            key.interestOps(SelectionKey.OP_WRITE);
          } else if (key.isWritable()) {
            var channel = (SocketChannel) key.channel();
            if (channel == client[0]) {
              backend[0].write(buffer);
            } else {
              client[0].write(buffer);
            }
            key.interestOps(SelectionKey.OP_READ);
          }
        } catch (Exception e) {
          System.out.println("I/O session error");
          e.printStackTrace();
          A4Io.close(backend[0]);
          backend[0] = null;
          A4Io.close(client[0]);
          client[0] = null;
          A4Io.zeroFill(buffer);
        }
      });
    }
  }
}
