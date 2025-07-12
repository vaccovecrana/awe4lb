package io.vacco.a4lb.tcp;

import io.vacco.a4lb.cfg.A4Backend;
import io.vacco.a4lb.niossl.*;
import io.vacco.a4lb.util.A4Io;
import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ExecutorService;

import static io.vacco.a4lb.util.A4Io.*;
import static java.lang.String.format;

public class A4TcpIo implements Closeable {

  public final String id;
  public final SelectionKey channelKey;
  public final SocketChannel channel;

  public A4Backend backend;
  private final ByteBuffer buffer = ByteBuffer.allocateDirect(256 * 1024);

  public boolean available = false;
  public boolean stalling = false;

  private static void setSocketOptions(Socket s) {
    try {
     s.setSoTimeout(5000); // TODO should these be configurable?
     s.setSoLinger(true, 5);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public A4TcpIo(SelectionKey channelKey, SocketChannel rawChannel) {
    this.channel = Objects.requireNonNull(rawChannel);
    this.channelKey = Objects.requireNonNull(channelKey);
    this.id = this.channel.socket().toString();
    setSocketOptions(this.channel.socket());
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
      setSocketOptions(this.channel.socket());
    } catch (Exception e) {
      throw new IllegalStateException("Server > Backend channel initialization error - " + dest, e);
    }
  }

  public int read() {
    if (available) {
      return 0; // Backpressure: don't read until current data is written
    }
    try {
      buffer.clear();
      int bytesRead = channel.read(buffer);
      if (bytesRead > 0) {
        buffer.flip();
        available = true;
      }
      return bytesRead;
    } catch (IOException ioe) {
      return -1;
    }
  }

  public int writeTo(ByteChannel channel) {
    int totalBytesWritten = 0;
    try {
      while (buffer.hasRemaining()) {
        int bytesWritten = channel.write(buffer);
        if (bytesWritten == 0) {
          this.stalling = true;
          break;
        } else if (bytesWritten > 0) {
          this.stalling = false;
        }
        totalBytesWritten += bytesWritten;
      }
    } catch (IOException e) {
      var msg = e.getMessage();
      if (msg != null && msg.contains("Broken pipe")) { // Client closed; treat as EOF
        return -1;
      } else {
        throw new IllegalStateException(e);
      }
    }
    if (!buffer.hasRemaining()) {
      available = false;
    }
    return totalBytesWritten;
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

  public void closeOutput() {
    try {
      this.channel.shutdownOutput();
    } catch (IOException e) {
      throw new IllegalStateException(e);
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
    this.backend = null;
  }

  @Override public String toString() {
    var k = this.channelKey;
    return format("[%d/%d]", k.interestOps(), k.readyOps());
  }

}