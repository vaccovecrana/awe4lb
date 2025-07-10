package io.vacco.a4lb.tcp;

import io.vacco.a4lb.cfg.A4Backend;
import io.vacco.a4lb.niossl.*;
import io.vacco.a4lb.util.A4Io;
import java.io.Closeable;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ExecutorService;

import static io.vacco.a4lb.util.A4Io.*;
import static java.lang.String.format;

public class A4TcpIo implements Closeable {

  public static final long    AdjustIntervalMs = 1000;
  public static final int     MinBufferSize = 32 * 1024;   // 32KB
  public static final int     MaxBufferSize = 1024 * 1024; // 1MB

  private static final int    QueuePressureIncrement = 4096;    // 4KB per queued buffer
  private static final int    MaxQueuePressureBoost = 32 * 1024; // 32KB max queue boost
  private static final double BufferSizeChangeThreshold = 0.25; // 25% change to adjust

  public static int MaxBackendBuffers = 8;  // Max queued buffers for backend
  public static int MaxClientBuffers = 8;  // Max queued buffers for client

  public final String id;
  public final SelectionKey channelKey;
  public final SocketChannel channel;

  private final Queue<ByteBuffer> bufferPool = new LinkedList<>();
  private final Queue<ByteBuffer> bufferQueue = new LinkedList<>();

  private int sendBufferSize;
  private int receiveBufferSize;
  public A4Backend backend;

  // Metrics for dynamic sizing
  private long totalBytesRead = 0;
  private long totalBytesWritten = 0;
  private long readCount = 0;
  private long writeCount = 0;
  private long lastAdjustTime = System.currentTimeMillis();

  public A4TcpIo(SelectionKey channelKey, SocketChannel rawChannel) {
    try {
      this.channel = Objects.requireNonNull(rawChannel);
      this.channelKey = Objects.requireNonNull(channelKey);
      this.id = this.channel.socket().toString();
      this.sendBufferSize = rawChannel.socket().getSendBufferSize();
      this.receiveBufferSize = rawChannel.socket().getReceiveBufferSize();
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
      this.sendBufferSize = chn.socket().getSendBufferSize();
      this.receiveBufferSize = chn.socket().getReceiveBufferSize();
    } catch (Exception e) {
      throw new IllegalStateException("Server > Backend channel initialization error - " + dest, e);
    }
  }

  public static int roundToPowerOfTwo(int value) {
    if (value <= 0) return MinBufferSize;
    int power = (int) Math.ceil(Math.log(value) / Math.log(2));
    return Math.min(MaxBufferSize, Math.max(MinBufferSize, 1 << power));
  }

  private void adjustBufferSize() {
    var now = System.currentTimeMillis();
    var timeElapsed = now - lastAdjustTime >= AdjustIntervalMs;
    var maxBuffers = (backend != null) ? MaxBackendBuffers : MaxClientBuffers;
    var highPressure = bufferQueue.size() > (maxBuffers * .75); // 75% of max
    if (!timeElapsed && !highPressure) {
      return;
    }
    lastAdjustTime = now;

    var avgBytesPerRead = readCount > 0 ? (double) totalBytesRead / readCount : 0;
    var avgBytesPerWrite = writeCount > 0 ? (double) totalBytesWritten / writeCount : 0;
    var queuePressure = bufferQueue.size();

    var targetReceiveSize = (int) Math.max(avgBytesPerRead, MinBufferSize);
    var targetSendSize = (int) Math.max(avgBytesPerWrite, MinBufferSize);

    targetReceiveSize += Math.min(queuePressure * QueuePressureIncrement, MaxQueuePressureBoost);
    targetSendSize += Math.min(queuePressure * QueuePressureIncrement, MaxQueuePressureBoost);
    targetReceiveSize = roundToPowerOfTwo(targetReceiveSize);
    targetSendSize = roundToPowerOfTwo(targetSendSize);

    var adjustReceive = Math.abs(targetReceiveSize - receiveBufferSize) > receiveBufferSize * BufferSizeChangeThreshold;
    var adjustSend = Math.abs(targetSendSize - sendBufferSize) > sendBufferSize * BufferSizeChangeThreshold;

    if (adjustReceive || adjustSend) {
      try {
        if (adjustReceive) {
          this.channel.socket().setReceiveBufferSize(targetReceiveSize);
        }
        if (adjustSend) {
          this.channel.socket().setSendBufferSize(targetSendSize);
        }
        if (log.isDebugEnabled()) {
          log.debug("[{}] Adjusting buffers: rx {} -> {} | tx {} -> {} (rxAvg={}, txAvg={}, qp={}, txBuf={}, rxBuf={})",
            id, receiveBufferSize, targetReceiveSize, sendBufferSize, targetSendSize,
            String.format("%.2f", avgBytesPerRead),
            String.format("%.2f", avgBytesPerWrite),
            queuePressure,
            this.channel.socket().getSendBufferSize(), this.channel.socket().getReceiveBufferSize());
        }
        if (adjustReceive) {
          receiveBufferSize = targetReceiveSize;
        }
        if (adjustSend) {
          sendBufferSize = targetSendSize;
        }
      } catch (SocketException e) {
        log.warn("[{}] Failed to adjust socket buffer sizes (rx={}, tx={}): {}",
          id, targetReceiveSize, targetSendSize, e.getMessage());
      }
    }
  }

  private ByteBuffer getBuffer() { // Sized for reading
    adjustBufferSize();
    var buffer = bufferPool.poll();
    if (buffer == null || buffer.capacity() != receiveBufferSize) {
      buffer = ByteBuffer.allocateDirect(receiveBufferSize);
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
      totalBytesRead += bytesRead;
      readCount++;
    } else {
      bufferPool.offer(buffer);
    }
    return bytesRead;
  }

  public int writeTo(ByteChannel channel) {
    int totalBytesWritten = 0;
    while (!bufferQueue.isEmpty()) {
      var buffer = bufferQueue.peek();
      var bytesWritten = eofWrite(channel, buffer);
      if (bytesWritten == 0) { // stalling...
        break;
      }
      totalBytesWritten += bytesWritten;
      if (!buffer.hasRemaining()) {
        bufferQueue.poll();
        bufferPool.offer(buffer); // Keep as-is, sized for receive
      } else {
        break;
      }
    }
    if (totalBytesWritten > 0) {
      this.totalBytesWritten += totalBytesWritten;
      writeCount++;
    }
    return totalBytesWritten;
  }

  public int writeTo(A4TcpIo target) {
    return writeTo(target.channel);
  }

  public boolean isStalling() {
    var maxBuffers = (backend != null) ? MaxBackendBuffers : MaxClientBuffers;
    return bufferQueue.size() >= maxBuffers;
  }

  public boolean isEmpty() {
    return this.bufferQueue.isEmpty();
  }

  public boolean available() {
    return !isEmpty();
  }

  public A4TcpIo writeable(boolean enable) {
    var k = this.channelKey;
    if (k.isValid()) {
      int currentOps = k.interestOps();
      if (enable) {
        k.interestOps(currentOps | SelectionKey.OP_WRITE);
      } else {
        k.interestOps(currentOps & ~SelectionKey.OP_WRITE);
      }
    }
    return this;
  }

  public boolean writeable() {
    return this.channelKey.isWritable();
  }

  public A4TcpIo readable(boolean enable) {
    var k = this.channelKey;
    if (k.isValid()) {
      int currentOps = k.interestOps();
      if (enable) {
        k.interestOps(currentOps | SelectionKey.OP_READ);
      } else {
        k.interestOps(currentOps & ~SelectionKey.OP_READ);
      }
    }
    return this;
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
    bufferPool.clear();
    bufferQueue.clear();
  }

  @Override public String toString() {
    var k = this.channelKey;
    return format(
      "[%d/%d, q%02d, p%02d, tx%d, rx%d]",
      k.interestOps(), k.readyOps(),
      bufferQueue.size(),
      bufferPool.size(),
      sendBufferSize, receiveBufferSize
    );
  }

}
