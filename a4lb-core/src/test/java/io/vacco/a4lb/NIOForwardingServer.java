package io.vacco.a4lb;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class NIOForwardingServer {

  public static int tryRead(SelectionKey k, SocketChannel c, ByteBuffer bb) throws IOException {
    bb.clear();
    int bytesRead = c.read(bb);
    if (bytesRead == -1) {
      System.out.printf("%s - channel EOF", k);
      c.close();
      k.cancel();
    } else if (bytesRead > 0) {
      bb.flip();
    }
    return bytesRead;
  }

  public static void main(String[] args) throws IOException {

    Selector selector = Selector.open();

    var ssc = ServerSocketChannel.open();
    ssc.bind(new InetSocketAddress(8080));
    ssc.configureBlocking(false);
    ssc.register(selector, SelectionKey.OP_ACCEPT);

    var bkc = SocketChannel.open();
    bkc.connect(new InetSocketAddress("localhost", 6900));
    bkc.configureBlocking(false);
    bkc.register(selector, SelectionKey.OP_READ);

    var buffer = ByteBuffer.allocate(8192);

    SocketChannel client = null;

    while (true) {
      selector.select();
      var keyIterator = selector.selectedKeys().iterator();

      while (keyIterator.hasNext()) {
        var key = keyIterator.next();
        keyIterator.remove();

        if (key.isAcceptable()) {
          var serverChannel = (ServerSocketChannel) key.channel();
          client = serverChannel.accept();
          client.configureBlocking(false);
          client.register(selector, SelectionKey.OP_READ);
        } else if (key.isReadable()) {
          var channel = (SocketChannel) key.channel();
          if (channel == client) { // data from client, fill buffer
            var bCount = tryRead(key, channel, buffer);
            if (bCount == -1) {
              continue;
            }
          } else { // data from backend, fill buffer
            var bCount = tryRead(key, bkc, buffer);
            if (bCount == -1) {
              continue;
            }
          }
          key.interestOps(SelectionKey.OP_WRITE);
        } else if (key.isWritable()) {
          var channel = (SocketChannel) key.channel();
          if (channel == client) { // client ready to receive, write buffer
            bkc.write(buffer);
          } else { // backend ready to receive, write buffer
            client.write(buffer);
          }
          key.interestOps(SelectionKey.OP_READ);
        }
      }
    }
  }
}
