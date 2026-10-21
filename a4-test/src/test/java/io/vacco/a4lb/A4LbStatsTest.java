package io.vacco.a4lb;

import com.google.gson.Gson;
import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.impl.A4Lb;
import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static j8spec.J8Spec.*;
import static org.junit.Assert.*;

@DefinedOrder
@RunWith(J8SpecRunner.class)
public class A4LbStatsTest {

  private static ServerSocket startEchoServer() throws IOException {
    var server = new ServerSocket(0);
    Thread.ofVirtual().name("a4-echo").start(() -> {
      while (!server.isClosed()) {
        try {
          var socket = server.accept();
          Thread.ofVirtual().name("a4-echo-session").start(() -> {
            try (socket) {
              socket.getInputStream().transferTo(socket.getOutputStream());
            } catch (IOException ignored) {
            }
          });
        } catch (IOException e) {
          break;
        }
      }
    });
    return server;
  }

  private static int freePort() throws IOException {
    try (var socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

  private static A4Stats waitFor(Supplier<A4Stats> supplier, Predicate<A4Stats> predicate, long timeoutMs)
      throws InterruptedException {
    var deadline = System.currentTimeMillis() + timeoutMs;
    A4Stats last = null;
    while (System.currentTimeMillis() < deadline) {
      last = supplier.get();
      if (predicate.test(last)) {
        return last;
      }
      Thread.sleep(100);
    }
    throw new AssertionError("Timed out waiting for stats. Last value: " + last);
  }

  static {
    it("Tracks TCP traffic through the proxy and exposes history", () -> {
      var echo = startEchoServer();
      var proxyPort = freePort();
      var payload = "hello-stats".getBytes(StandardCharsets.UTF_8);
      var backend = new A4Backend()
        .addr(new A4Sock().host("127.0.0.1").port(echo.getLocalPort()))
        .state(A4BackendState.Up);
      var config = new A4Config()
        .id("stats-test")
        .description("statistics integration test")
        .server(new A4Server()
          .id("tcp")
          .addr(new A4Sock().host("127.0.0.1").port(proxyPort))
          .match(new A4Match().pool(new A4Pool().hosts(backend))));

      var lb = new A4Lb(config, new Gson());
      try {
        lb.open();
        try (var client = new Socket("127.0.0.1", proxyPort)) {
          client.setSoTimeout(5000);
          client.getOutputStream().write(payload);
          client.getOutputStream().flush();
          var response = new byte[payload.length];
          var read = client.getInputStream().read(response);
          assertEquals(payload.length, read);
          assertArrayEquals(payload, response);
        }

        var stats = waitFor(lb::stats, s ->
          s.backends.size() == 1
            && s.backends.get(0).txBytes >= payload.length
            && s.backends.get(0).rxBytes >= payload.length
            && s.backends.get(0).totalConnections >= 1, 8000);

        var b = stats.backends.get(0);
        assertEquals("tcp", b.serverId);
        assertEquals("TCP", b.serverType);
        assertEquals("127.0.0.1", b.host);
        assertEquals(echo.getLocalPort(), b.port);
        assertTrue(b.txBytes >= payload.length);
        assertTrue(b.rxBytes >= payload.length);

        waitFor(lb::stats, s -> s.backends.get(0).activeConnections == 0, 5000);
        Thread.sleep(1200);
        var withHistory = lb.stats();
        assertTrue(withHistory.history.timestamps.size() >= 2);
        assertEquals(1, withHistory.history.series.size());
        assertEquals(b.id, withHistory.history.series.get(0).id);
        assertEquals(withHistory.history.timestamps.size(), withHistory.history.series.get(0).rxBps.length);
      } finally {
        lb.close();
        echo.close();
      }
    });
  }
}
