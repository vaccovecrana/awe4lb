package io.vacco.a4lb;

import org.slf4j.*;
import stormpot.*;

import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.*;

public class A4Lb implements Closeable {

  private static final Logger log = LoggerFactory.getLogger(A4Lb.class);

  private final ServerSocket serverSocket;
  private final Timeout to = new Timeout(1, TimeUnit.SECONDS);
  private final Pool<A4PTcp> tcpPool = Pool
      .from(new A4PTcpAlloc())
      .setExpiration(new A4PTcpExp())
      .build();

  public A4Lb() {
    try {
      // TODO config param
      int port = 12345;
      this.serverSocket = new ServerSocket(port); // TODO what about SSL server connections?
      log.info("Server listening on port {}", port);
    } catch (Exception e) {
      throw new IllegalStateException("Balancer initialization error", e);
    }
  }

  public void start() {
    try {
      var mon = new A4PTcpMon();
      while (true) {
        var clientSocket = serverSocket.accept();
        try {
          var backendPs = tcpPool.claim(to);
          mon.accept(clientSocket, backendPs);
        } catch (Exception e) {
          log.error("Unable to allocate backend socket.", e);
          A4Io.close(clientSocket);
        }
      }
    } catch (IOException e) {
      log.error("Server connection error", e);
    }
  }

  public void stop() {
    try {
      log.info("Stopping.");
      serverSocket.close();
    } catch (Exception e) {
      log.error("Server stop error", e);
    }
  }

  @Override public void close() { stop(); }

}

