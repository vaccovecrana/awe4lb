package io.vacco.a4lb;

import org.slf4j.*;
import java.nio.channels.*;
import java.util.Objects;

import static io.vacco.a4lb.A4Io.*;

public class A4TcpSess {

  private static final Logger log = LoggerFactory.getLogger(A4TcpSess.class);

  private final A4TcpCl client;
  private final A4TcpBk backend;
  public  final A4TcpSrv owner;

  public A4TcpSess(A4TcpSrv owner, A4TcpCl client, A4TcpBk backend) {
    this.client = Objects.requireNonNull(client);
    this.backend = Objects.requireNonNull(backend);
    this.owner = Objects.requireNonNull(owner);
    client.channelKey.attach(this);
    backend.channelKey.attach(this);
  }

  private void tearDown(Exception e) {
    if (e != null && log.isTraceEnabled()) {
      log.trace(
          "{} - {} - abnormal session termination",
          client.channel.socket(), backend.channel.socket(), e
      );
    }
    close(client.channelKey, client.channel);
    close(backend.channelKey, backend.channel);
  }

  private void sessionMismatch(SelectionKey key) {
    throw new IllegalStateException("key/session mismatch " + key);
  }

  public void update(SelectionKey key) {
    try {
      var channel = (SocketChannel) key.channel();
      if (key.isReadable()) {
        if (channel == client.channel) {
          if (eofRead(client.channel, backend.buffer) == -1) {
            tearDown(null);
            return;
          }
        } else if (channel == backend.channel) {
          if (eofRead(backend.channel, backend.buffer) == -1) {
            tearDown(null);
            return;
          }
        } else {
          sessionMismatch(key);
        }
        key.interestOps(SelectionKey.OP_WRITE);
      } else if (key.isWritable()) {
        if (channel == client.channel) {
          backend.channel.write(backend.buffer);
        } else if (channel == backend.channel) {
          client.channel.write(backend.buffer);
        } else {
          sessionMismatch(key);
        }
        key.interestOps(SelectionKey.OP_READ);
      } else {
        throw new IllegalStateException(channel.socket() + " - Unexpected channel key state");
      }
    } catch (Exception e) {
      tearDown(e);
    }
  }

}
