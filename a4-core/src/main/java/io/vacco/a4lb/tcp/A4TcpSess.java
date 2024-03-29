package io.vacco.a4lb.tcp;

import io.vacco.a4lb.niossl.*;
import io.vacco.a4lb.sel.A4Selector;
import org.slf4j.*;
import javax.net.ssl.*;
import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ExecutorService;

import static io.vacco.a4lb.util.A4Io.*;
import static io.vacco.a4lb.util.A4Exceptions.rootCauseOf;
import static java.lang.String.format;

public class A4TcpSess extends SNIMatcher {

  public enum IOOp { Read, Write }

  private static final Logger log = LoggerFactory.getLogger(A4TcpSess.class);

  public  A4TcpSrv owner;
  private final A4Selector bkSel;

  private A4TcpIo client, backend;
  private String id;

  private String tlsSni;
  private final ExecutorService tlsExec;
  private final boolean tlsClient;

  private final Queue<ByteBuffer> cltQ = new ArrayDeque<>(); // TODO check if buffer pooling could increase performance after running initial benchmarks.
  private final Queue<ByteBuffer> bckQ = new ArrayDeque<>();

  public A4TcpSess(A4TcpSrv owner, A4Selector bkSel, boolean tlsClient, ExecutorService tlsExec) {
    super(0);
    this.owner = Objects.requireNonNull(owner);
    this.bkSel = Objects.requireNonNull(bkSel);
    this.tlsClient = tlsClient;
    this.tlsExec = tlsExec;
  }

  private void tearDown(Exception e) {
    if (e != null) {
      var x = rootCauseOf(e);
      if (log.isDebugEnabled()) {
        log.debug("!! [{}, {}] {} - {} - {}",
            client != null ? client.id : "?",
            backend != null ? backend.id : "?",
            e.getClass().getSimpleName(), x.getClass().getSimpleName(),
            e == x ? e.getMessage() : format("%s - %s", e.getMessage(), x.getMessage())
        );
      } else if (log.isTraceEnabled()) {
        log.trace("!! [{}, {}] {} - {} - {}",
            client != null ? client.id : "?",
            backend != null ? backend.id : "?",
            e.getClass().getSimpleName(), x.getClass().getSimpleName(),
            e == x ? e.getMessage() : format("%s - %s", e.getMessage(), x.getMessage()),
            x
        );
      }
    }
    if (client != null) { client.close(); }
    if (backend != null) {
      backend.close();
      bkSel.stateOf(backend.backend).trackConn(false);
    }
    cltQ.clear();
    bckQ.clear();
    this.owner = null;
    if (log.isDebugEnabled()) {
      log.debug("------------------------------");
    }
  }

  private void logState(int bytes, ByteChannel c, IOOp op) {
    if (log.isDebugEnabled() && bytes != 0) {
      var sck = c instanceof SSLSocketChannel
          ? ((SSLSocketChannel) c).getWrappedSocketChannel().socket()
          : ((SocketChannel) c).socket();
      log.debug("{}: {} (i/o: {}), c[{},{},{}] b[{},{},{}] {} {} {}",
          this.id != null ? id : '?',
          op == IOOp.Read ? 'r' : 'w',
          format("%06d", bytes),
          format("%02d", client.channelKey.interestOps()),
          format("%02d", client.channelKey.readyOps()),
          format("%02d", cltQ.size()),
          backend != null ? format("%02d", backend.channelKey.interestOps()) : "?",
          backend != null ? format("%02d", backend.channelKey.readyOps()) : "?",
          format("%02d", bckQ.size()),
          sck.getLocalSocketAddress(),
          op == IOOp.Read ? "<<" : ">>",
          sck.getRemoteSocketAddress()
      );
    }
  }

  private int doTcpRead(String channelId, ByteChannel from, Queue<ByteBuffer> target, int bufferSize) {
    var bb = ByteBuffer.allocateDirect(bufferSize);
    var bytes = eofRead(channelId, from, bb);
    logState(bytes, from, IOOp.Read);
    if (bytes > 0) {
      target.add(bb);
    }
    return bytes;
  }

  private int doTcpWrite(ByteChannel to, Queue<ByteBuffer> source) throws IOException {
    var bb = source.peek();
    if (bb != null) {
      var bytes = to.write(bb);
      logState(bytes, to, IOOp.Write);
      if (!bb.hasRemaining()) {
        source.remove();
      }
      return bytes;
    }
    throw new IllegalStateException(to + " - no data available for writing");
  }

  private void updateQueue(Queue<ByteBuffer> q, SelectionKey k) {
    if (!q.isEmpty()) {
      k.interestOps(SelectionKey.OP_WRITE);
    } else if (k.interestOps() != 0) {
      k.interestOps(SelectionKey.OP_READ);
    }
  }

  private void syncOps(SelectionKey key, IOOp op, int bytes) {
    if (op == IOOp.Read && backend != null && key == backend.channelKey && bytes > 0) {
      bkSel.stateOf(backend.backend).trackRxTx(false, bytes);
    }
    if (op == IOOp.Write && backend != null && key == backend.channelKey && bytes > 0) {
      bkSel.stateOf(backend.backend).trackRxTx(true, bytes);
    }
    if (op == IOOp.Read && key == client.channelKey && bytes > 0 && backend == null) {
      initBackend(key);
    }
    if (bytes == -1) {
      if (client.channelKey == key) {
        client.channelKey.interestOps(0);
      } else if (backend != null && backend.channelKey == key) {
        backend.channelKey.interestOps(0);
      }
      return;
    }
    updateQueue(cltQ, client.channelKey);
    if (backend != null) {
      updateQueue(bckQ, backend.channelKey);
    }
  }

  private void onTcpRead(SelectionKey key) throws SocketException {
    var bytes = -1;
    if (key == client.channelKey) {
      int rcv = client.channel.socket().getReceiveBufferSize();
      bytes = doTcpRead(client.id, client.channel, bckQ, rcv); // reverse targets, this is intentional
    } else if (key == backend.channelKey) {
      int rcv = backend.channel.socket().getReceiveBufferSize();
      bytes = doTcpRead(backend.id, backend.channel, cltQ, rcv);
    } else {
      sessionMismatch(key);
    }
    syncOps(key, IOOp.Read, bytes);
  }

  private void onTcpWrite(SelectionKey key) throws IOException {
    int bytes = -1;
    if (key == client.channelKey) {
      bytes = doTcpWrite(client.channel, cltQ);
    } else if (key == backend.channelKey) {
      bytes = doTcpWrite(backend.channel, bckQ);
    } else {
      sessionMismatch(key);
    }
    syncOps(key, IOOp.Write, bytes);
  }

  @Override public boolean matches(SNIServerName sn) {
    var sni = SSLCertificates.sniOf(sn).orElseThrow();
    var op = bkSel.matches(client.channel, sni);
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
    this.backend = bkSel.assign(key.selector(), client.channel, tlsSni, tlsExec);
    this.backend.channelKey.attach(this);
    this.id = format("%x", format("%s-%s",
        client.getRawChannel().socket(),
        backend.getRawChannel().socket()
    ).hashCode());
    this.bkSel.stateOf(backend.backend).trackConn(true);
  }

  private void tcpUpdate(SelectionKey key) throws IOException {
    if (key.isReadable()) {
      onTcpRead(key);
    } else if (key.isWritable()) {
      onTcpWrite(key);
    } else {
      throw new IllegalStateException(((SocketChannel) key.channel()).socket() + " - Unexpected channel key state");
    }
    if (client.channelKey.interestOps() == 0 && backend != null && backend.channelKey.interestOps() == 0) {
      tearDown(null);
    } else if (client.channelKey.interestOps() == 0 && cltQ.isEmpty()) {
      tearDown(null);
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
