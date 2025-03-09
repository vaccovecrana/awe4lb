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

  public final String id;
  public final SelectionKey channelKey;
  public final SocketChannel channel;

  private final Queue<ByteBuffer> bufferPool = new LinkedList<>();
  public  final Queue<ByteBuffer> bufferQueue = new LinkedList<>();
  private final int bufferSize;

  public A4Backend backend;

  public A4TcpIo(SelectionKey channelKey, SocketChannel rawChannel) {
    try {
      this.channel = Objects.requireNonNull(rawChannel);
      this.channelKey = Objects.requireNonNull(channelKey);
      this.id = this.channel.socket().toString();
      this.bufferSize = rawChannel.socket().getReceiveBufferSize();
    } catch (Exception e) {
      throw new IllegalStateException("Client > Server channel initialization error - " + rawChannel.socket(), e);
    }
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
      this.bufferSize = chn.socket().getReceiveBufferSize();
    } catch (Exception e) {
      throw new IllegalStateException("Server > Backend channel initialization error - " + dest, e);
    }
  }

  private ByteBuffer getBuffer() {
    var buffer = bufferPool.poll();
    if (buffer == null) {
      buffer = ByteBuffer.allocateDirect(bufferSize);
    } else {
      buffer.clear();
    }
    return buffer;
  }

  public int read() {
    var buffer = getBuffer();
    int bytesRead = eofRead(this.channel, buffer);
    if (bytesRead > 0) {
      bufferQueue.offer(buffer);
    } else {
      bufferPool.offer(buffer);
    }
    return bytesRead;
  }

  public int writeTo(ByteChannel channel) {
    int totalBytesWritten = 0;
    while (!bufferQueue.isEmpty()) {
      var buffer = bufferQueue.peek();
      int bytesWritten = eofWrite(channel, buffer);
      totalBytesWritten += bytesWritten;
      if (!buffer.hasRemaining()) {
        bufferQueue.poll();
        bufferPool.offer(buffer);
      } else {
        break;
      }
    }
    return totalBytesWritten;
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
    bufferPool.clear();
    bufferQueue.clear();
  }

  @Override public String toString() {
    var k = this.channelKey;
    var c = this.channel;
    var sck = c instanceof SSLSocketChannel
      ? ((SSLSocketChannel) c).getWrappedSocketChannel().socket()
      : c.socket();
    return format(
      "[%s, bq%02d, bp%02d, i%02d, r%02d, %s %s %s]",
      format("%s%s%s%s",
        k.isReadable() ? "r" : "",
        k.isWritable() ? "w" : "",
        k.isConnectable() ? "c" : "",
        k.isAcceptable() ? "a" : ""
      ),
      bufferQueue.size(),
      bufferPool.size(),
      k.interestOps(), k.readyOps(),
      sck.getLocalSocketAddress(),
      k.isReadable() ? "<" : k.isWritable() ? ">" : "<?>",
      sck.getRemoteSocketAddress()
    );
  }

}
