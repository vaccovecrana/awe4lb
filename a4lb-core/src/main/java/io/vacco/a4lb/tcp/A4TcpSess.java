package io.vacco.a4lb.tcp;

import io.vacco.a4lb.niossl.SSLSocketChannel;
import org.slf4j.*;
import javax.net.ssl.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import static io.vacco.a4lb.tcp.A4Io.*;
import static io.vacco.a4lb.util.A4Exceptions.rootCauseOf;
import static java.lang.String.format;

public class A4TcpSess extends SNIMatcher {

  public enum IOOp { Read, Write }

  private static final Logger log = LoggerFactory.getLogger(A4TcpSess.class);

  public final A4TcpSrv owner;
  public final ByteBuffer buffer;

  private A4TcpIo client, backend;
  private String id;

  private String tlsSni;
  private final ExecutorService tlsExec;
  private final boolean tlsClient;

  public A4TcpSess(A4TcpSrv owner, int bufferSize, boolean tlsClient, ExecutorService tlsExec) {
    super(0); // TODO wat???
    this.owner = Objects.requireNonNull(owner);
    this.buffer = ByteBuffer.allocateDirect(bufferSize);
    this.tlsClient = tlsClient;
    this.tlsExec = tlsExec;
  }

  private void sessionMismatch(SelectionKey key) {
    throw new IllegalStateException("key/session mismatch " + key);
  }

  private void tearDown(Exception e) {
    if (e != null) {
      var x = rootCauseOf(e);
      if (log.isDebugEnabled()) {
        log.debug("!! [{}, {}] {} - {} - {}",
            client != null ? client.id : "?",
            backend != null ? backend.id : "?",
            e.getClass().getSimpleName(), x.getClass().getSimpleName(), x.getMessage());
      } else if (log.isTraceEnabled()) {
        log.trace("!! [{}, {}] {} - {} - {}",
            client != null ? client.id : "?",
            backend != null ? backend.id : "?",
            e.getClass().getSimpleName(), x.getClass().getSimpleName(), x.getMessage(), x);
      }
    }
    if (client != null) { client.close(); }
    if (backend != null) { backend.close(); }
    if (log.isDebugEnabled()) {
      log.debug("------------------------------");
    }
  }

  private void logState(int bytes, ByteChannel c, IOOp op) {
    if (log.isDebugEnabled()) {
      var sck = c instanceof SSLSocketChannel
          ? ((SSLSocketChannel) c).getWrappedSocketChannel().socket()
          : ((SocketChannel) c).socket();
      log.debug("{}: {} (i/o: {}, r: {}), c[{},{}] b[{},{}] {} {} {}",
          this.id != null ? id : '?',
          op == IOOp.Read ? 'r' : 'w',
          format("%06d", bytes),
          format("%06d", buffer.remaining()),
          format("%02d", client.channelKey.interestOps()),
          format("%02d", client.channelKey.readyOps()),
          backend != null ? format("%02d", backend.channelKey.interestOps()) : "??",
          backend != null ? format("%02d", backend.channelKey.readyOps()) : "??",
          sck.getLocalSocketAddress(),
          op == IOOp.Read ? "<<" : ">>",
          sck.getRemoteSocketAddress()
      );
    }
  }

  private int doTcpRead(String channelId, ByteChannel from) {
    var readBytes = eofRead(channelId, from, buffer);
    logState(readBytes, from, IOOp.Read);
    return readBytes;
  }

  private int doTcpWrite(ByteChannel to) throws IOException {
    var writtenBytes = to.write(buffer);
    logState(writtenBytes, to, IOOp.Write);
    return writtenBytes;
  }

  private void syncOps(SelectionKey key, IOOp op, int bytes) {
    if (bytes == -1) {
      tearDown(null);
      return;
    }
    if (op == IOOp.Read && backend != null && key == backend.channelKey && bytes > 0) {
      backend.target.rxTx.updateTx(bytes);
    }
    if (op == IOOp.Write && backend != null && key == backend.channelKey && bytes > 0) {
      backend.target.rxTx.updateRx(bytes);
    }
    if (op == IOOp.Read && bytes == 0) {
      buffer.limit(buffer.position());
    }
    if (bytes == 0 && backend != null && key == backend.channelKey) { // TODO offload client TCP buffer full
      log.warn("client slow down??");
      try { Thread.sleep(1000); }
      catch (InterruptedException e) { throw new RuntimeException(e); }
    }
    if (bytes == 0 && key == client.channelKey && buffer.hasRemaining()) { // TODO offload backend TCP buffer full
      log.warn("backend slow down??");
      try {
        Thread.sleep(1000);
        doTcpWrite(backend.channel);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    if (op == IOOp.Read && buffer.hasRemaining()) {
      key.interestOps(SelectionKey.OP_WRITE);
    }
    if (op == IOOp.Write && !buffer.hasRemaining()) {
      key.interestOps(SelectionKey.OP_READ);
    }
  }

  private void onTcpRead(SelectionKey key) {
    int bytes = -1;
    if (key == client.channelKey) {
      bytes = doTcpRead(client.id, client.channel);
    } else if (key == backend.channelKey) {
      bytes = doTcpRead(backend.id, backend.channel);
    } else {
      sessionMismatch(key);
    }
    syncOps(key, IOOp.Read, bytes);
  }

  private void onTcpWrite(SelectionKey key) throws IOException {
    int bytes = -1;
    if (key == client.channelKey) {
      if (backend == null) {
        initBackend(key);
      }
      bytes = doTcpWrite(backend.channel);
    } else if (key == backend.channelKey) {
      bytes = doTcpWrite(client.channel);
    } else {
      sessionMismatch(key);
    }
    syncOps(key, IOOp.Write, bytes);
  }

  @Override public boolean matches(SNIServerName sn) {
    var sni = A4Ssl.sniOf(sn).orElseThrow();
    var op = owner.bkSelect.matches(client.channel, sni);
    if (op.isPresent()) {
      this.tlsSni = sni;
      return true;
    }
    return false;
  }

  private void initBackend(SelectionKey key) {
    if (tlsClient && tlsSni == null) {
      return;
    }
    this.backend = owner.bkSelect.assign(key.selector(), client.channel, tlsSni, tlsExec);
    this.backend.channelKey.attach(this);
    this.id = format("%x", format("%s-%s",
        client.getRawChannel().socket(),
        backend.getRawChannel().socket()
    ).hashCode());
  }

  private void tcpUpdate(SelectionKey key) throws IOException {
    if (key.isReadable()) {
      onTcpRead(key);
    } else if (key.isWritable()) {
      onTcpWrite(key);
    } else {
      throw new IllegalStateException(((SocketChannel) key.channel()).socket() + " - Unexpected channel key state");
    }
  }

  public void update(SelectionKey key) {
    try {
      if (key.attachment() == this) {
        tcpUpdate(key);
      } else {
        sessionMismatch(key);
      }
    } catch (Exception e) {
      tearDown(e);
    }
  }

  public void setClient(A4TcpIo client) {
    this.client = Objects.requireNonNull(client);
    this.client.channelKey.attach(this);
  }

  /*
   * The Oceanview Motel and Casino is a familiar friend to me. I stayed in countless motels like it
   * while investigating A.W.E’s across the country, back in my field agent days. Those roadside motels
   * all bleed together like a dream, same and not the same, anywhere and nowhere.
   *
   * The Oceanview operates on dream-logic, and the light-switch cord leaks out to be found in the most
   * unexpected places, and sometimes, successfully encouraged to appear and act as a convenient lock
   * to keep out those not trained in dreamscape navigation.
   *
   * Even Bureau veterans can only find one key in the Motel, the key that opens the door marked with
   * the inverted Black Pyramid. The rest, the many other doors, are still mysteries to us.
   *
   * We’re all merely guests there. Even the Board. Sometimes I need to visit, just to breathe easier
   * for a while. It beats the numb sterile apartment I spend my nights in, insulated from everything
   * but myself. I guess that’s where the whiskey comes in.
   */

}
