package io.vacco.a4lb;

import org.slf4j.*;
import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.util.Random;
import java.util.concurrent.*;

public class A4Lb implements Closeable {

  private static final Logger log = LoggerFactory.getLogger(A4Lb.class);

  private final int port = 12345; // Change this to your desired port number
  private final ExecutorService executor = Executors.newCachedThreadPool();

  private final ServerSocket serverSocket;
  private final Random rnd = new Random();

  public A4Lb() {
    try {
      this.serverSocket = new ServerSocket(port); // TODO what about SSL server connections?
      log.info("Server listening on port {}", port);
    } catch (Exception e) {
      throw new IllegalStateException("Balancer initialization error", e);
    }
  }

  public void start() {
    try (var backend = new Socket("127.0.0.1", 6900)) { // TODO config param

      backend.setTcpNoDelay(true);

      while (true) {
        var clientSocket = serverSocket.accept();

        clientSocket.setTcpNoDelay(true);

        executor.submit(() -> {
          var a4 = new A4Tcp(clientSocket, backend);

          // TODO pull a backend socket here...

          var forwardExecutor = Executors.newFixedThreadPool(
              2, r -> new Thread(r, String.format("a4lb-%x", rnd.nextInt()))
          );

          // TODO I think it's really the Future what can be used to determine which I/O task has finished
          //   and then, based on that, stop/interrupt the pending Future (i.e. returning backend connections to the socket pool).
          forwardExecutor.submit(a4.c2b());
          forwardExecutor.submit(a4.b2c());

          CompletableFuture.anyOf(
              // TODO here...
          );
        });
      }
    } catch (IOException e) {
      log.error("Server connection error", e);
    }
  }

  public void stop() {
    try {
      log.info("Stopping.");
      executor.shutdown();
      serverSocket.close();
    } catch (Exception e) {
      log.error("Server stop error", e);
    }
  }

  @Override public void close() { stop(); }

}

