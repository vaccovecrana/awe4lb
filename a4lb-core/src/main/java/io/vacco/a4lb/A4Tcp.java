package io.vacco.a4lb;

import org.slf4j.*;
import java.io.*;
import java.net.Socket;
import java.util.Objects;

class A4Tcp implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(A4Tcp.class);

  private final Socket in, out; // TODO rename these to client and backend.

  /*
   * TODO
   *   These should be instead DTOs with metadata.
   *   Track the connection direction. Both tasks will need to exchange messages (possibly become actors).
   *   If the C2B actor fails, notify B2C actor to stop, and close the client socket (trace log).
   *   If the B2C actor fails, notify C2B actor to stop, and close both sockets (info log).
   */
  public A4Tcp(Socket in, Socket out) {
    this.in = Objects.requireNonNull(in);
    this.out = Objects.requireNonNull(out);
  }

  // TODO
  //   it may be better to have this class produce two Runnables, C2B and B2C, which should help implement the logic above.
  //   split the code below into C2B and B2C communication logic.
  @Override public void run() {
    var cid = A4Io.connId(in, out); // TODO extract this to a transfer(in, out) function at A4Io (static), return # of bytes transferred.
    long bytes = -1;
    try {
      var is = in.getInputStream();
      var os = out.getOutputStream();
      bytes = is.transferTo(os);
    } catch (IOException e) {
      log.error("{} - Forward connection error", cid, e);
    } finally {
      A4Io.close(in);
      A4Io.close(out);
      if (log.isTraceEnabled()) {
        log.trace("{} - {} bytes transferred: ", cid, bytes);
      }
    }
  }
}
