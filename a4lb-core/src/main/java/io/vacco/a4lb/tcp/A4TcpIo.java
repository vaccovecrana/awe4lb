package io.vacco.a4lb.tcp;

import io.vacco.a4lb.cfg.A4Backend;
import tlschannel.*;
import java.io.Closeable;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Objects;

public class A4TcpIo implements Closeable {

  public final String id;
  public final SelectionKey channelKey;
  public final SocketChannel channel;
  public final TlsChannel tlsChannel;

  public A4Backend target;

  public A4TcpIo(Selector selector, SocketChannel rawChannel, TlsChannel tlsChannel) {
    try {
      this.channel = Objects.requireNonNull(rawChannel);
      this.tlsChannel = tlsChannel;
      this.channelKey = this.channel.register(selector, SelectionKey.OP_READ);
      this.id = this.channel.socket().toString();
    } catch (Exception e) {
      throw new IllegalStateException("Client-Server channel initialization error - " + rawChannel.socket(), e);
    }
  }

  public A4TcpIo(InetSocketAddress dest, Selector selector) {
    try {
      this.tlsChannel = null; // TODO implement backend side TLS channel initialization.
      this.channel = SocketChannel.open();
      this.channel.connect(dest);
      this.channel.configureBlocking(false);
      this.channelKey = channel.register(selector, SelectionKey.OP_READ);
      this.id = channel.socket().toString();
    } catch (Exception e) {
      throw new IllegalStateException("Server-Backend channel initialization error - " + dest, e);
    }
  }

  public A4TcpIo target(A4Backend backend) {
    this.target = Objects.requireNonNull(backend);
    this.target.trackConnOpen();
    return this;
  }

  @Override public void close() {
    channelKey.attach(null);
    channelKey.cancel();
    A4Io.close(channel);
    if (target != null) {
      this.target.trackConnClose();
      this.target = null;
    }
  }

}
