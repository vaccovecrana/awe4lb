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

  private void doTcpRead(String channelId, ByteChannel from) {
    if (eofRead(channelId, from, buffer) == -1) {
      tearDown(null);
    }
  }

  private void onTcpRead(SelectionKey key, ByteChannel channel) {
    if (log.isTraceEnabled()) {
      log.trace("<<<< {}", key.channel());
    }
    if (channel == client.channel) {
      if (client.tlsChannel != null) {
        doTcpRead(client.id, client.tlsChannel);
      } else {
        doTcpRead(client.id, client.channel);
      }
    } else if (channel == backend.channel) {
      // TODO add case for reading from TLS backend channel too.
      doTcpRead(backend.id, backend.channel);
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
      backend.channel.write(buffer);
    } else if (channel == backend.channel) {
      if (client.tlsChannel != null) {
        client.tlsChannel.write(buffer);
      } else {
        client.channel.write(buffer);
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

}
