package io.vacco.a4lb;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class A4TcpBk {

  public final String id;
  public final SelectionKey channelKey;
  public final SocketChannel channel;
  public ByteBuffer buffer;

  public A4TcpBk(InetSocketAddress dest, Selector selector, int bufferSize) {
    try {
      this.channel = SocketChannel.open();
      this.channel.connect(dest);
      this.channel.configureBlocking(false);
      this.channelKey = channel.register(selector, SelectionKey.OP_READ);
      this.buffer = ByteBuffer.allocateDirect(bufferSize);
      this.id = channel.socket().toString();
    } catch (Exception e) {
      throw new IllegalStateException("Backend channel initialization error - " + dest, e);
    }
  }

}
