package io.vacco.a4lb.tcp;

import org.slf4j.*;
import tlschannel.*;

import javax.net.ssl.SSLEngineResult;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Objects;

import static io.vacco.a4lb.tcp.A4Io.*;

public class A4TcpSess {

  private static final Logger log = LoggerFactory.getLogger(A4TcpSess.class);

  public final A4TcpSrv owner;
  public final ByteBuffer buffer;

  private A4TcpIo client, backend;
  private String tlsSni;

  public A4TcpSess(A4TcpSrv owner, int bufferSize) {
    this.owner = Objects.requireNonNull(owner);
    this.buffer = ByteBuffer.allocateDirect(bufferSize);
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
    if (updateStats) {
      backend.target.rxTx.updateTx(readBytes);
    }
    if (readBytes == -1) {
      tearDown(null);
    }
  }

  private void doTcpWrite(ByteChannel to, ByteBuffer b, boolean updateStats) throws IOException {
    var writtenBytes = to.write(b);
    if (updateStats) {
      backend.target.rxTx.updateRx(writtenBytes);
    }
  }

  private void onTcpRead(SelectionKey key, ByteChannel channel) {
    if (log.isTraceEnabled()) {
      log.trace("<<<< {}", key.channel());
    }
    if (channel == client.channel) {
      if (client.tlsChannel != null) {
        doTcpRead(client.id, client.tlsChannel, false);
      } else {
        doTcpRead(client.id, client.channel, false);
      }
    } else if (channel == backend.channel) {
      // TODO add case for reading from TLS backend channel too.
      doTcpRead(backend.id, backend.channel, true);
    } else {
      sessionMismatch(key);
    }
    if (key.isValid()) {
      key.interestOps(SelectionKey.OP_WRITE);
    }
  }

  private void onTcpWrite(SelectionKey key, ByteChannel channel) throws IOException {
    if (log.isTraceEnabled()) {
      log.trace(">>>> {}", key.channel());
    }
    if (channel == client.channel) {
      // TODO add case for writing from TLS backend channel too.
      doTcpWrite(backend.channel, buffer, false);
    } else if (channel == backend.channel) {
      if (client.tlsChannel != null) {
        doTcpWrite(client.tlsChannel, buffer, true);
      } else {
        doTcpWrite(client.channel, buffer, true);
      }
    } else {
      sessionMismatch(key);
    }
    if (key.isValid()) {
      key.interestOps(SelectionKey.OP_READ);
    }
  }

  private void initBackend(SelectionKey key) {
    if (client.tlsChannel == null) {
      this.backend = owner.backendPool.get(key.selector(), client.channel, null);
    } else if (client.tlsChannel.getSslEngine() != null) {
      var hsStat = client.tlsChannel.getSslEngine().getHandshakeStatus();
      if (hsStat == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
        this.backend = owner.backendPool.get(key.selector(), client.channel, tlsSni);
      }
    }
    if (this.backend != null) {
      this.backend.channelKey.attach(this);
    }
  }

  private void tcpUpdate(SelectionKey key) throws IOException {
    var channel = (SocketChannel) key.channel();
    if (backend == null && channel == client.channel) {
      initBackend(key);
    }
    if (key.isReadable()) {
      onTcpRead(key, channel);
    } else if (key.isWritable()) {
      onTcpWrite(key, channel);
    } else {
      throw new IllegalStateException(channel.socket() + " - Unexpected channel key state");
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
      var x = getRootException(e);
      if (x instanceof NeedsReadException) {
        key.interestOps(SelectionKey.OP_READ);
      } else if (x instanceof NeedsWriteException) {
        key.interestOps(SelectionKey.OP_WRITE);
      } else {
        tearDown(e);
      }
    }
  }

  public void setTlsSni(String tlsSni) {
    this.tlsSni = Objects.requireNonNull(tlsSni);
  }

  public void setClient(A4TcpIo client) {
    this.client = Objects.requireNonNull(client);
    this.client.channelKey.attach(this);
  }

  /*
   * The Oceanview Motel and Casino is a familiar friend to me. I stayed in countless motels like it
   * while investigating A.W.E’s across the country, back in my field agent days.
   *
   * Those roadside motels all bleed together like a dream, same and not the same, anywhere and nowhere.
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
