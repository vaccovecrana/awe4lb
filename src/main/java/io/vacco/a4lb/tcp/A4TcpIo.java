package io.vacco.a4lb.tcp;

import io.vacco.a4lb.cfg.A4Backend;
import io.vacco.a4lb.niossl.SSLSocketChannel;
import io.vacco.a4lb.util.A4Io;
import java.io.Closeable;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

public class A4TcpIo implements Closeable {

  public final String id;
  public final SelectionKey channelKey;
  public final SocketChannel channel;

  public A4Backend backend;

  public A4TcpIo(SelectionKey channelKey, SocketChannel rawChannel) {
    try {
      this.channel = Objects.requireNonNull(rawChannel);
      this.channelKey = Objects.requireNonNull(channelKey);
      this.id = this.channel.socket().toString();
    } catch (Exception e) {
      throw new IllegalStateException("Client-Server channel initialization error - " + rawChannel.socket(), e);
    }
  }

  public A4TcpIo(InetSocketAddress dest, Selector selector, boolean openTls, ExecutorService tlsExec) {
    try {
      var chn = SocketChannel.open();
      if (openTls) {
        var ctx = A4Ssl.trustAllContext();
        var eng = ctx.createSSLEngine();
        eng.setUseClientMode(true);
        this.channel = new SSLSocketChannel(chn, eng, tlsExec);
      } else {
        this.channel = chn;
      }
      this.channel.connect(dest);
      this.channel.configureBlocking(false);
      this.channelKey = chn.register(selector, SelectionKey.OP_READ);
      this.id = channel.socket().toString();
    } catch (Exception e) {
      throw new IllegalStateException("Server-Backend channel initialization error - " + dest, e);
    }
  }

  public A4TcpIo backend(A4Backend backend) {
    this.backend = Objects.requireNonNull(backend);
    return this;
  }

  public SocketChannel getRawChannel() {
    return channel instanceof SSLSocketChannel
        ? ((SSLSocketChannel) channel).getWrappedSocketChannel()
        : channel;
  }

  @Override public void close() {
    channelKey.attach(null);
    channelKey.cancel();
    A4Io.close(channel);
    if (backend != null) {
      this.backend = null;
    }
  }

}
