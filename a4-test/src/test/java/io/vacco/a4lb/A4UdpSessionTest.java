package io.vacco.a4lb;

import com.google.gson.Gson;
import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.impl.A4Lb;
import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static j8spec.J8Spec.it;
import static org.junit.Assert.assertEquals;

@DefinedOrder
@RunWith(J8SpecRunner.class)
public class A4UdpSessionTest {

  private static int freeUdpPort() throws Exception {
    try (var socket = new DatagramSocket(0, InetAddress.getByName("127.0.0.1"))) {
      return socket.getLocalPort();
    }
  }

  private static DatagramSocket echo(String prefix, List<Thread> threads) throws Exception {
    var socket = new DatagramSocket(0, InetAddress.getByName("127.0.0.1"));
    var thread = Thread.ofVirtual().name("a4-udp-echo-" + prefix).start(() -> {
      var buffer = new byte[2048];
      while (!socket.isClosed()) {
        try {
          var packet = new DatagramPacket(buffer, buffer.length);
          socket.receive(packet);
          var payload = prefix + ":" + new String(
            packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8
          );
          var response = payload.getBytes(StandardCharsets.UTF_8);
          socket.send(new DatagramPacket(response, response.length, packet.getAddress(), packet.getPort()));
        } catch (Exception e) {
          break;
        }
      }
    });
    threads.add(thread);
    return socket;
  }

  private static String exchange(DatagramSocket client, int port, String payload) throws Exception {
    var bytes = payload.getBytes(StandardCharsets.UTF_8);
    client.send(new DatagramPacket(bytes, bytes.length, new InetSocketAddress("127.0.0.1", port)));
    var response = new DatagramPacket(new byte[256], 256);
    client.receive(response);
    return new String(response.getData(), response.getOffset(), response.getLength(), StandardCharsets.UTF_8);
  }

  static {
    it("Keeps a UDP session alive across the configured idle timeout", () -> {
      var threads = new ArrayList<Thread>();
      var echo0 = echo("A", threads);
      var echo1 = echo("B", threads);
      var client = new DatagramSocket();
      client.setSoTimeout(2000);

      var proxyPort = freeUdpPort();
      var udp = new A4Udp();
      udp.bufferSize = 2048;
      udp.idleTimeoutMs = 1500;
      udp.maxSessions = 32;

      var pool = new A4Pool().hosts(
        new A4Backend().addr(new A4Sock().host("127.0.0.1").port(echo0.getLocalPort())),
        new A4Backend().addr(new A4Sock().host("127.0.0.1").port(echo1.getLocalPort()))
      );
      pool.type = A4PoolType.roundRobin;

      var server = new A4Server()
        .id("udp-session")
        .addr(new A4Sock().host("127.0.0.1").port(proxyPort))
        .match(new A4Match().pool(pool));
      server.udp = udp;

      var config = new A4Config()
        .id("udp-session")
        .description("UDP sliding session timeout")
        .server(server);

      var lb = new A4Lb(config, new Gson());
      try {
        lb.open();
        assertEquals("A:first", exchange(client, proxyPort, "first"));

        Thread.sleep(300);
        assertEquals("A:second", exchange(client, proxyPort, "second"));

        // The original session would expire at 1500ms. The second packet
        // refreshed it to 1800ms, so this packet must still use backend A.
        Thread.sleep(1300);
        assertEquals("A:third", exchange(client, proxyPort, "third"));
      } finally {
        lb.close();
        client.close();
        echo0.close();
        echo1.close();
        for (var thread : threads) {
          thread.join(1000);
        }
      }
    });
  }
}
