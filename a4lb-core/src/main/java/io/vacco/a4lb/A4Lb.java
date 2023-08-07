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
  private final Socket backendSocket;
  private final Random rnd = new Random();

  public A4Lb() {
    try {
      this.serverSocket = new ServerSocket(port); // TODO what about SSL server connections?
      this.backendSocket = new Socket("127.0.0.1", 6900); // TODO config param
      log.info("Server listening on port {}", port);
    } catch (Exception e) {
      throw new IllegalStateException("Balancer initialization error", e);
    }
  }

  public void start() {
    try {
      while (true) {
        Socket clientSocket = serverSocket.accept();
        executor.submit(() -> {
          A4Forward clientToBackend = new A4Forward(clientSocket, backendSocket);
          A4Forward backendToClient = new A4Forward(backendSocket, clientSocket);
          ExecutorService forwardExecutor = Executors.newFixedThreadPool(
              2, r -> new Thread(r, String.format("a4lb-%x", rnd.nextInt()))
          );
          forwardExecutor.submit(clientToBackend);
          forwardExecutor.submit(backendToClient);
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
      backendSocket.close();
    } catch (Exception e) {
      log.error("Server stop error", e);
    }
  }

  @Override public void close() { stop(); }

}

