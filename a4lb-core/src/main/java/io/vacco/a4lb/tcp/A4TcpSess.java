package io.vacco.a4lb.tcp;

import org.slf4j.*;
import javax.net.ssl.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import static io.vacco.a4lb.tcp.A4Io.*;
import static io.vacco.a4lb.util.A4Exceptions.rootCauseOf;

public class A4TcpSess extends SNIMatcher {

  private static final Logger log = LoggerFactory.getLogger(A4TcpSess.class);

  public final A4TcpSrv owner;
  public final ByteBuffer buffer;

  private A4TcpIo client, backend;

  private ExecutorService tlsExec;
  private String tlsSni;
  private boolean isTls;

  public A4TcpSess(A4TcpSrv owner, int bufferSize, boolean isTls, ExecutorService tlsExec) {
    super(0); // TODO wat???
    this.owner = Objects.requireNonNull(owner);
    this.buffer = ByteBuffer.allocateDirect(bufferSize);
    this.isTls = isTls;
    this.tlsExec = Objects.requireNonNull(tlsExec);
  }

  private void sessionMismatch(SelectionKey key) {
    throw new IllegalStateException("key/session mismatch " + key);
  }

  private void tearDown(Exception e) {
    if (e != null && log.isTraceEnabled()) {
      log.trace(
          "[{}] - [{}] - abnormal session termination",
          client != null ? client.id : "?",
          backend != null ? backend.id : "?", e
      );
    }
    if (client != null) { client.close(); }
    if (backend != null) { backend.close(); }
  }

  private void doTcpRead(String channelId, ByteChannel from, boolean updateStats) {
    var readBytes = eofRead(channelId, from, buffer);
    if (log.isDebugEnabled()) {
      log.debug("R [{}] <<<< {}", readBytes, from);
    }
    if (updateStats) {
      backend.target.rxTx.updateTx(readBytes);
    }
    if (readBytes == -1) {
      tearDown(null);
    }
  }

  private void doTcpWrite(ByteChannel to, ByteBuffer b, boolean updateStats) throws IOException {
    var writtenBytes = to.write(b);
    if (log.isDebugEnabled()) {
      log.debug("W [{}] >>>> {}", writtenBytes, to);
    }
    if (updateStats) {
      backend.target.rxTx.updateRx(writtenBytes);
    }
  }

  private void onTcpRead(SelectionKey key) {
    if (key == client.channelKey) {
      doTcpRead(client.id, client.channel, false);
    } else if (key == backend.channelKey) {
      doTcpRead(backend.id, backend.channel, true);
    } else {
      sessionMismatch(key);
    }
    if (key.isValid()) {
      key.interestOps(SelectionKey.OP_WRITE);
    }
  }

  private void onTcpWrite(SelectionKey key) throws IOException {
    if (key == client.channelKey) {
      if (backend == null) {
        initBackend(key);
      }
      doTcpWrite(backend.channel, buffer, false);
      if (backend.channelKey.interestOps() == 0) {
        backend.channelKey.interestOps(SelectionKey.OP_WRITE);
      }
    } else if (key == backend.channelKey) {
      doTcpWrite(client.channel, buffer, true);
    } else {
      sessionMismatch(key);
    }
    if (key.isValid()) {
      key.interestOps(SelectionKey.OP_READ);
    }
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
    if (isTls && tlsSni == null) {
      return;
    }
    this.backend = owner.bkSelect.assign(key.selector(), client.channel, tlsSni, tlsExec);
    this.backend.channelKey.attach(this);
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

  private void onSessionError(SelectionKey key, Exception e, Throwable x) {
    // TODO clean this up.
    log.warn("!!! - {} - {}", e.getClass().getSimpleName(), x.getClass().getSimpleName());
    tearDown(e);
  }

  public void update(SelectionKey key) {
    try {
      if (key.attachment() == this) {
        tcpUpdate(key);
      } else {
        sessionMismatch(key);
      }
    } catch (Exception e) {
      onSessionError(key, e, rootCauseOf(e));
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
