package io.vacco.a4lb;

import org.slf4j.*;
import java.nio.channels.*;
import java.util.Objects;

import static io.vacco.a4lb.A4Io.*;

public class A4TcpSess {

  private static final Logger log = LoggerFactory.getLogger(A4TcpSess.class);

  private final SocketChannel client;
  private final A4TcpBk backend;
  private final Runnable onTearDown;

  public A4TcpSess(SocketChannel client, A4TcpBk backend, Runnable onTearDown) {
    this.backend = Objects.requireNonNull(backend);
    this.client = Objects.requireNonNull(client);
    this.onTearDown = Objects.requireNonNull(onTearDown);
  }

  private void tearDown(Exception e) {
    if (e != null && log.isTraceEnabled()) {
      log.trace(
          "{} - {} - abnormal session termination",
          client.socket(), backend.channel.socket(), e
      );
    }
    close(client);
    if (!backend.channel.isConnected()) {
      backend.expire();
    }
    backend.release();
    onTearDown.run();
  }

  private void sessionMismatch(SelectionKey key) {
    throw new IllegalStateException("key/session mismatch " + key);
  }

  public void update(SelectionKey key) {
    try {
      if (key.isReadable()) {
        if (key.channel() == client) {
          if (eofRead(key, client, backend.buffer) == -1) {
            tearDown(null);
            return;
          }
        } else if (key.channel() == backend.channel) {
          if (eofRead(key, backend.channel, backend.buffer) == -1) {
            tearDown(null);
            return;
          }
        } else {
          sessionMismatch(key);
        }
        key.interestOps(SelectionKey.OP_WRITE);
      } else if (key.isWritable()) {
        var channel = (SocketChannel) key.channel();
        if (channel == client) {
          backend.channel.write(backend.buffer);
        } else if (channel == backend.channel) {
          client.write(backend.buffer);
        } else {
          sessionMismatch(key);
        }
        key.interestOps(SelectionKey.OP_READ);
      }
    } catch (Exception e) {
      tearDown(e);
    }
  }

}
