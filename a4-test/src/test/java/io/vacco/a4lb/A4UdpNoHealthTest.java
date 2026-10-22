package io.vacco.a4lb;

import com.google.gson.Gson;
import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.impl.A4Lb;
import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static j8spec.J8Spec.*;
import static org.junit.Assert.*;

@DefinedOrder
@RunWith(J8SpecRunner.class)
public class A4UdpNoHealthTest {

  private static int freeUdpPort() throws Exception {
    try (var socket = new DatagramSocket(0, InetAddress.getByName("127.0.0.1"))) {
      return socket.getLocalPort();
    }
  }

  static {
    it("Routes UDP traffic when no health check is configured", () -> {
      var echo = new DatagramSocket(0, InetAddress.getByName("127.0.0.1"));
      var echoThread = Thread.ofVirtual().name("a4-udp-echo").start(() -> {
        var buffer = new byte[2048];
        while (!echo.isClosed()) {
          try {
            var packet = new DatagramPacket(buffer, buffer.length);
            echo.receive(packet);
            echo.send(new DatagramPacket(packet.getData(), packet.getLength(), packet.getAddress(), packet.getPort()));
          } catch (Exception e) {
            break;
          }
        }
      });

      var proxyPort = freeUdpPort();
      var udp = new A4Udp();
      udp.bufferSize = 2048;
      udp.idleTimeoutMs = 2000;
      udp.maxSessions = 128;

      var server = new A4Server()
        .id("udp-no-health")
        .addr(new A4Sock().host("127.0.0.1").port(proxyPort))
        .match(new A4Match().pool(new A4Pool().hosts(
          new A4Backend().addr(new A4Sock().host("127.0.0.1").port(echo.getLocalPort()))
        )));
      server.udp = udp;

      var config = new A4Config()
        .id("udp-no-health")
        .description("UDP without a health check")
        .server(server);

      var lb = new A4Lb(config, new Gson());
      try {
        lb.open();
        assertEquals("Up", lb.snapshot().backends.get(0).state);

        var payload = "udp-no-health".getBytes(StandardCharsets.UTF_8);
        try (var client = new DatagramSocket()) {
          client.setSoTimeout(5000);
          client.send(new DatagramPacket(payload, payload.length,
            new InetSocketAddress("127.0.0.1", proxyPort)));
          var buffer = new byte[128];
          var response = new DatagramPacket(buffer, buffer.length);
          client.receive(response);
          assertArrayEquals(payload, Arrays.copyOf(buffer, response.getLength()));
        }

        var deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
          var backend = lb.snapshot().backends.get(0);
          if (backend.txBytes > 0 && backend.rxBytes > 0) {
            break;
          }
          Thread.sleep(50);
        }
        var backend = lb.snapshot().backends.get(0);
        assertTrue(backend.txBytes > 0);
        assertTrue(backend.rxBytes > 0);
        assertEquals("Up", backend.state);
      } finally {
        lb.close();
        echo.close();
        echoThread.join(1000);
      }
    });
  }
}
