package io.vacco.a4lb.tcp;

import io.vacco.a4lb.cfg.A4Backend;
import io.vacco.a4lb.niossl.*;
import io.vacco.a4lb.util.A4Io;
import java.io.Closeable;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ExecutorService;

import static io.vacco.a4lb.util.A4Io.*;
import static java.lang.String.format;

public class A4TcpIo implements Closeable {

  public static final ByteBuffer Empty = ByteBuffer.allocateDirect(0);

  public final String id;
  public final SelectionKey channelKey;
  public final SocketChannel channel;

  public A4Backend backend;
  private final ByteBuffer buffer = ByteBuffer.allocateDirect(128 * 1024);

  public boolean available = false;
  public boolean stalling = false;

  public A4TcpIo(SelectionKey channelKey, SocketChannel rawChannel) {
    this.channel = Objects.requireNonNull(rawChannel);
    this.channelKey = Objects.requireNonNull(channelKey);
    this.id = this.channel.socket().toString();
  }

  public A4TcpIo(InetSocketAddress dest, Selector selector, boolean openTls, ExecutorService tlsExec) {
    try {
      var chn = SocketChannel.open();
      if (openTls) {
        var ctx = SSLCertificates.trustAllContext();
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
      throw new IllegalStateException("Server > Backend channel initialization error - " + dest, e);
    }
  }

  public int read() {
    if (available) {
      return 0; // Backpressure: don't read until current data is written
    }
    int bytesRead = eofRead(this.channel, buffer);
    if (bytesRead > 0) {
      available = true;
    }
    return bytesRead;
  }

  public int writeTo(ByteChannel channel) {
    int totalBytesWritten = 0;
    while (buffer.hasRemaining()) {
      int bytesWritten = eofWrite(channel, buffer);
      if (bytesWritten == 0) {
        this.stalling = true;
        break; // Can't write more right now
      } else if (bytesWritten > 0) {
        this.stalling = false;
      }
      totalBytesWritten += bytesWritten;
    }
    if (!buffer.hasRemaining()) {
      available = false;
    }
    return totalBytesWritten;
  }

  public int writeEmpty() {
    return eofWrite(this.channel, Empty);
  }

  public int writeTo(A4TcpIo target) {
    return writeTo(target.channel);
  }

  public void writeable(boolean enable) {
    var k = this.channelKey;
    if (k.isValid()) {
      int currentOps = k.interestOps();
      if (enable) {
        k.interestOps(currentOps | SelectionKey.OP_WRITE);
      } else {
        k.interestOps(currentOps & ~SelectionKey.OP_WRITE);
      }
    }
  }

  public boolean writeable() {
    return this.channelKey.isWritable();
  }

  public void readable(boolean enable) {
    var k = this.channelKey;
    if (k.isValid()) {
      int currentOps = k.interestOps();
      if (enable) {
        k.interestOps(currentOps | SelectionKey.OP_READ);
      } else {
        k.interestOps(currentOps & ~SelectionKey.OP_READ);
      }
    }
  }

  public boolean readable() {
    return this.channelKey.isReadable();
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
    this.backend = null;
  }

  @Override public String toString() {
    var k = this.channelKey;
    return format("[%d/%d]", k.interestOps(), k.readyOps());
  }

}