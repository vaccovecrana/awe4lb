package io.vacco.a4lb;

import org.slf4j.*;
import java.net.Socket;
import java.util.Objects;

class A4Tcp {

  private static final Logger log = LoggerFactory.getLogger(A4Tcp.class);

  private final Socket client, backend;

  /*
   * TODO
   *   These should be instead DTOs with metadata.
   *   Track the connection direction. Both tasks will need to exchange messages (possibly become actors).
   *   If the C2B actor fails, notify B2C actor to stop, and close the client socket (trace log).
   *   If the B2C actor fails, notify C2B actor to stop, and close both sockets (info log).
   */
  public A4Tcp(Socket client, Socket backend) {
    this.client = Objects.requireNonNull(client);
    this.backend = Objects.requireNonNull(backend);
  }

  private void ioCheck(String cid, long bytes) {
    if (log.isTraceEnabled()) {
      log.trace("{} - {} bytes transferred", cid, bytes);
    }
    A4Io.close(client);
    if (!A4Io.isSocketUsable(backend)) {
      log.warn("{} - Backend connection error", cid);
      A4Io.close(backend); // TODO most likely will need to notify a backend down error here.
      A4Io.close(client);
    }
    if (log.isTraceEnabled()) {
      log.trace("{} - EOF", cid);
    }
  }

  public Runnable c2b() {
    return () -> A4Io.io(client, backend, this::ioCheck);
  }

  public Runnable b2c() {
    return () -> A4Io.io(backend, client, this::ioCheck);
  }
}
