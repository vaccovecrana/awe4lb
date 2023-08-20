package io.vacco.a4lb;

import tlschannel.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Objects;

import static io.vacco.a4lb.A4Io.*;

public class A4TcpSess {

  private final A4TcpIo client, backend;
  public  final A4TcpSrv owner;
  public final ByteBuffer buffer;

  public A4TcpSess(A4TcpSrv owner, A4TcpIo client, A4TcpIo backend, int bufferSize) {
    this.client = Objects.requireNonNull(client);
    this.backend = Objects.requireNonNull(backend);
    this.owner = Objects.requireNonNull(owner);
    this.buffer = ByteBuffer.allocateDirect(bufferSize);
    client.channelKey.attach(this);
    backend.channelKey.attach(this);
  }

  private void sessionMismatch(SelectionKey key) {
    throw new IllegalStateException("key/session mismatch " + key);
  }

  private void tearDown(Exception e) {
    if (e != null && log.isTraceEnabled()) {
      log.trace("{} - {} - abnormal session termination", client.id, backend.id, e);
    }
    client.close();
    backend.close();
  }

  private void doTcpRead(String channelId, ByteChannel from) {
    if (eofRead(channelId, from, buffer) == -1) {
      tearDown(null);
    }
  }

  private void onTcpRead(SelectionKey key, ByteChannel channel) {
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

  private void tcpUpdate(SelectionKey key) throws IOException {
    var channel = (SocketChannel) key.channel();
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
      if (e.getCause() instanceof NeedsReadException) {
        key.interestOps(SelectionKey.OP_READ);
      } else {
        tearDown(e);
      }
    }
  }

}
