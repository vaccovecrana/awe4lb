package io.vacco.a4lb;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Objects;

import static io.vacco.a4lb.A4Io.*;

public class A4TcpSess {

  private final A4TcpIo client;
  private final A4TcpIo backend;
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

  private void doTcpRead(SelectionKey key, A4TcpIo from) {
    if (eofRead(from.id, from.channel, buffer) == -1) {
      tearDown(null);
    } else if (key.isValid()) {
      key.interestOps(SelectionKey.OP_WRITE);
    }
  }

  private void onTcpRead(SelectionKey key, SocketChannel channel) {
    if (channel == client.channel) {
      doTcpRead(key, client);
    } else if (channel == backend.channel) {
      doTcpRead(key, backend);
    } else {
      sessionMismatch(key);
    }
  }

  private void onTcpWrite(SelectionKey key, SocketChannel channel) throws IOException {
    if (channel == client.channel) {
      backend.channel.write(buffer);
    } else if (channel == backend.channel) {
      client.channel.write(buffer);
    } else {
      sessionMismatch(key);
    }
    key.interestOps(SelectionKey.OP_READ);
  }

  private void tcpUpdate(SelectionKey key) {
    try {
      var channel = (SocketChannel) key.channel();
      if (key.isReadable()) {
        onTcpRead(key, channel);
      } else if (key.isWritable()) {
        onTcpWrite(key, channel);
      } else {
        throw new IllegalStateException(channel.socket() + " - Unexpected channel key state");
      }
    } catch (Exception e) {
      tearDown(e);
    }
  }

  public void update(SelectionKey key) {
    if (key.attachment() == this) {
      if (client.tlsChannel == null) {
        tcpUpdate(key);
      } // else { } TODO so how do we handle TLS???
    } else {
      sessionMismatch(key);
    }
  }

}
