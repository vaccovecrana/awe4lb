package io.vacco.a4lb;

import java.nio.channels.*;
import java.util.Objects;

public class A4TcpCl {

  public final SelectionKey channelKey;
  public final SocketChannel channel;

  // TODO add TLS attachments here.

  public A4TcpCl(ServerSocketChannel source, Selector selector) {
    try {
      this.channel = Objects.requireNonNull(source).accept();
      this.channel.configureBlocking(false);
      this.channelKey = this.channel.register(selector, SelectionKey.OP_READ);
    } catch (Exception e) {
      throw new IllegalStateException("Client channel initialization error - " + source.socket(), e);
    }
  }

}
