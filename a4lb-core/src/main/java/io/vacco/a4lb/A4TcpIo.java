package io.vacco.a4lb;

import tlschannel.*;
import javax.net.ssl.SSLContext;
import java.io.Closeable;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Objects;

public class A4TcpIo implements Closeable {

  public final String id;
  public final SelectionKey channelKey;
  public final SocketChannel channel;
  public final TlsChannel tlsChannel;

  public A4TcpIo(ServerSocketChannel source, Selector selector, SSLContext sslContext) {
    try {
      this.channel = Objects.requireNonNull(source).accept();
      this.channel.configureBlocking(false);
      this.tlsChannel = sslContext == null
          ? null
          : ServerTlsChannel.newBuilder(this.channel, sslContext).withRunTasks(false).build();
      this.channelKey = this.channel.register(selector, SelectionKey.OP_READ);
      this.id = this.channel.socket().toString();
    } catch (Exception e) {
      throw new IllegalStateException("Client channel initialization error - " + source.socket(), e);
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
      throw new IllegalStateException("Backend channel initialization error - " + dest, e);
    }
  }

  @Override public void close() {
    channelKey.attach(null);
    channelKey.cancel();
    A4Io.close(channel);
  }

}
