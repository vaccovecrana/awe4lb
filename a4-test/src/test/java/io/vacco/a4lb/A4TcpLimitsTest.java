package io.vacco.a4lb;

import com.google.gson.Gson;
import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.impl.A4Lb;
import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.BooleanSupplier;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLSocket;

import io.vacco.a4lb.tcp.A4TlsCerts;
import static j8spec.J8Spec.it;
import static org.junit.Assert.*;

@DefinedOrder
@RunWith(J8SpecRunner.class)
public class A4TcpLimitsTest {

  private static int freeTcpPort() throws Exception {
    try (var socket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
      return socket.getLocalPort();
    }
  }

  private static A4Config tcpConfig(int proxyPort, int maxPerIp) {
    var tcp = new A4Tcp();
    tcp.maxConnectionsPerIp = maxPerIp;

    var server = new A4Server()
      .id("tcp-limits")
      .addr(new A4Sock().host("127.0.0.1").port(proxyPort))
      .match(new A4Match().pool(new A4Pool().hosts(
        new A4Backend().addr(new A4Sock().host("127.0.0.1").port(1)).state(A4BackendState.Up)
      )));
    server.tcp = tcp;

    return new A4Config()
      .id("tcp-limits")
      .description("TCP limits")
      .server(server);
  }

  private static void await(String message, BooleanSupplier condition) throws Exception {
    var deadline = System.currentTimeMillis() + 3000;
    while (System.currentTimeMillis() < deadline) {
      if (condition.getAsBoolean()) {
        return;
      }
      Thread.sleep(20);
    }
    fail(message);
  }

  static {
    it("Rejects connections above the per-IP limit and releases the slot on close", () -> {
      var proxyPort = freeTcpPort();
      var lb = new A4Lb(tcpConfig(proxyPort, 1), new Gson());
      Socket first = null;
      Socket second = null;
      try {
        lb.open();
        first = new Socket("127.0.0.1", proxyPort);
        second = new Socket("127.0.0.1", proxyPort);
        second.setSoTimeout(2000);

        await("second connection was not rejected", () -> {
          var server = lb.snapshot().servers.get(0);
          return server.activeConnections == 1
            && server.rejectedConnections == 1
            && "TCP".equals(server.serverType);
        });
        assertEquals(-1, second.getInputStream().read());

        second.close();
        first.close();
        first = null;
        second = null;
        await("connection slot was not released", () -> lb.snapshot().servers.get(0).activeConnections == 0);
      } finally {
        if (first != null) {
          first.close();
        }
        if (second != null) {
          second.close();
        }
        lb.close();
      }
    });

    it("Closes accepted sessions that never reached backend selection on shutdown", () -> {
      var proxyPort = freeTcpPort();
      var lb = new A4Lb(tcpConfig(proxyPort, 4), new Gson());
      Socket client = null;
      try {
        lb.open();
        client = new Socket("127.0.0.1", proxyPort);
        client.setSoTimeout(2000);
        await("session was not registered", () -> lb.snapshot().servers.get(0).activeConnections == 1);
        lb.close();
        assertEquals(-1, client.getInputStream().read());
      } finally {
        if (client != null) {
          client.close();
        }
        lb.close();
      }
    });

    it("Times out a TLS handshake that never sends a ClientHello", () -> {
      var proxyPort = freeTcpPort();
      var cfg = tcpConfig(proxyPort, 4);
      var tcp = cfg.servers.get(0).tcp;
      tcp.tlsHandshakeTimeoutMs = 200;
      tcp.maxTlsHandshakes = 1;

      var tls = new A4ServerTls();
      tls.base = new A4Tls();
      tls.base.certPath = "./src/test/resources/certs/awe4lb.pem";
      tls.base.keyPath = "./src/test/resources/certs/awe4lb.key";
      cfg.servers.get(0).tls = tls;

      var lb = new A4Lb(cfg, new Gson());
      Socket client = null;
      try {
        lb.open();
        client = new Socket("127.0.0.1", proxyPort);
        client.setSoTimeout(3000);
        await("handshake timeout was not counted", () ->
          lb.snapshot().servers.get(0).tlsHandshakeTimeouts == 1
            && "TLS".equals(lb.snapshot().servers.get(0).serverType)
        );
      } finally {
        if (client != null) {
          client.close();
        }
        lb.close();
      }
    });

    it("Rejects TLS handshakes above the concurrent limit", () -> {
      var proxyPort = freeTcpPort();
      var cfg = tcpConfig(proxyPort, 4);
      var tcp = cfg.servers.get(0).tcp;
      tcp.tlsHandshakeTimeoutMs = 5000;
      tcp.maxTlsHandshakes = 1;

      var tls = new A4ServerTls();
      tls.base = new A4Tls();
      tls.base.certPath = "./src/test/resources/certs/awe4lb.pem";
      tls.base.keyPath = "./src/test/resources/certs/awe4lb.key";
      cfg.servers.get(0).tls = tls;

      var lb = new A4Lb(cfg, new Gson());
      Socket first = null;
      Socket second = null;
      try {
        lb.open();
        first = new Socket("127.0.0.1", proxyPort);
        await("first TLS handshake was not acquired", () ->
          lb.snapshot().servers.get(0).activeTlsHandshakes == 1
        );
        second = new Socket("127.0.0.1", proxyPort);
        await("second TLS handshake was not rejected", () ->
          lb.snapshot().servers.get(0).rejectedTlsHandshakes == 1
        );
      } finally {
        if (first != null) {
          first.close();
        }
        if (second != null) {
          second.close();
        }
        lb.close();
      }
    });

    it("Completes a TLS handshake and routes client data", () -> {
      var backend = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
      var echoThread = Thread.ofVirtual().name("a4-tcp-echo").start(() -> {
        try (var socket = backend.accept()) {
          var buffer = new byte[128];
          var read = socket.getInputStream().read(buffer);
          socket.getOutputStream().write(buffer, 0, read);
          socket.getOutputStream().flush();
        } catch (Exception e) {
          // test teardown
        }
      });

      var proxyPort = freeTcpPort();
      var cfg = tcpConfig(proxyPort, 4);
      cfg.servers.get(0).match.get(0).pool.hosts.get(0).addr.port = backend.getLocalPort();

      var tls = new A4ServerTls();
      tls.base = new A4Tls();
      tls.base.certPath = "./src/test/resources/certs/awe4lb.pem";
      tls.base.keyPath = "./src/test/resources/certs/awe4lb.key";
      cfg.servers.get(0).tls = tls;

      var lb = new A4Lb(cfg, new Gson());
      SSLSocket client = null;
      try {
        lb.open();
        client = (SSLSocket) A4TlsCerts.trustAllContext().getSocketFactory()
          .createSocket("127.0.0.1", proxyPort);
        var params = client.getSSLParameters();
        params.setServerNames(List.of(new SNIHostName("test.localhost")));
        client.setSSLParameters(params);
        client.setSoTimeout(3000);
        client.startHandshake();

        var payload = "hello".getBytes(StandardCharsets.UTF_8);
        client.getOutputStream().write(payload);
        client.getOutputStream().flush();
        assertEquals("hello", new String(client.getInputStream().readNBytes(payload.length), StandardCharsets.UTF_8));
      } finally {
        if (client != null) {
          client.close();
        }
        lb.close();
        backend.close();
        echoThread.join(1000);
      }
    });
  }
}
