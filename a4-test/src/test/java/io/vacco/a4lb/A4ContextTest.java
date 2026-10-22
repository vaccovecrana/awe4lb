package io.vacco.a4lb;

import com.google.gson.Gson;
import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.service.A4Context;
import io.vacco.a4lb.util.A4Options;
import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static j8spec.J8Spec.*;
import static org.junit.Assert.*;

@DefinedOrder
@RunWith(J8SpecRunner.class)
public class A4ContextTest {

  private static int[] freeTcpPorts(int count) throws Exception {
    var sockets = new ServerSocket[count];
    try {
      for (int i = 0; i < count; i++) {
        sockets[i] = new ServerSocket(0);
      }
      var ports = new int[count];
      for (int i = 0; i < count; i++) {
        ports[i] = sockets[i].getLocalPort();
      }
      return ports;
    } finally {
      for (var socket : sockets) {
        if (socket != null) {
          socket.close();
        }
      }
    }
  }

  private static Path writeConfig(Path dir, String id, int proxyPort) throws Exception {
    var config = new A4Config()
      .id(id)
      .description("context test")
      .server(new A4Server()
        .id("tcp-" + id)
        .addr(new A4Sock().host("127.0.0.1").port(proxyPort))
        .match(new A4Match().pool(new A4Pool().hosts(
          new A4Backend().addr(new A4Sock().host("127.0.0.1").port(1))
        ))));
    var file = dir.resolve(id + ".json");
    Files.writeString(file, new Gson().toJson(config));
    return file;
  }

  private static HttpResponse<String> get(int apiPort, String path) throws Exception {
    var request = HttpRequest.newBuilder()
      .uri(URI.create("http://127.0.0.1:" + apiPort + path))
      .GET()
      .build();
    return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
  }

  static {
    it("Keeps the UI/API available when the startup configuration is invalid", () -> {
      var dir = Files.createTempDirectory("a4-context-invalid");
      var invalid = dir.resolve("broken.json");
      Files.writeString(invalid, "{not valid json");
      var apiPort = freeTcpPorts(1)[0];
      var ctx = new A4Context();
      try {
        ctx.init(A4Options.from(new String[]{
          A4Options.flagOf(A4Options.kConfig, invalid.toString()),
          A4Options.flagOf(A4Options.kApiPort, Integer.toString(apiPort))
        }));
        assertNull(ctx.service.instance);
        assertEquals(200, get(apiPort, "/api/v1/config").statusCode());
        assertEquals(404, get(apiPort, "/api/v1/config?configId=..%2Fescape").statusCode());
      } finally {
        ctx.close();
      }
    });

    it("Fails fast when the configured path is missing", () -> {
      var dir = Files.createTempDirectory("a4-context-missing");
      var missing = dir.resolve("active.json");
      var apiPort = freeTcpPorts(1)[0];
      var ctx = new A4Context();
      try {
        assertThrows(IllegalArgumentException.class, () -> ctx.init(A4Options.from(new String[]{
          A4Options.flagOf(A4Options.kConfig, missing.toString()),
          A4Options.flagOf(A4Options.kApiPort, Integer.toString(apiPort))
        })));
      } finally {
        ctx.close();
      }
    });

    it("Starts from a file and still allows stop and alternate selection", () -> {
      var dir = Files.createTempDirectory("a4-context-file");
      var ports = freeTcpPorts(3);
      var boot = writeConfig(dir, "boot", ports[0]);
      var alt = writeConfig(dir, "alt", ports[1]);
      assertTrue(Files.exists(alt));
      var apiPort = ports[2];
      var ctx = new A4Context();
      try {
        ctx.init(A4Options.from(new String[]{
          A4Options.flagOf(A4Options.kConfig, boot.toString()),
          A4Options.flagOf(A4Options.kApiPort, Integer.toString(apiPort))
        }));
        assertNotNull(ctx.service.instance);
        assertEquals("boot", ctx.service.instance.config.id);

        assertEquals(200, get(apiPort, "/api/v1/config/select").statusCode());
        assertNull(ctx.service.instance);

        assertEquals(200, get(apiPort, "/api/v1/config/select?configId=alt").statusCode());
        assertNotNull(ctx.service.instance);
        assertEquals("alt", ctx.service.instance.config.id);
      } finally {
        ctx.close();
      }
    });
  }
}
