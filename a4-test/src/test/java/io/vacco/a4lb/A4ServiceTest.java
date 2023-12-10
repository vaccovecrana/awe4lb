package io.vacco.a4lb;

import com.github.mizosoft.methanol.*;
import com.google.gson.Gson;
import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.niossl.SSLCertificates;
import io.vacco.a4lb.service.A4Context;
import io.vacco.a4lb.util.*;
import io.vacco.a4lb.web.A4Route;
import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.buildobjects.process.ProcBuilder;
import org.junit.runner.RunWith;
import org.slf4j.*;
import javax.net.ssl.SSLContext;
import java.io.IOException;

import static io.vacco.a4lb.web.A4Route.*;
import static io.vacco.a4lb.util.A4Flags.*;
import static j8spec.J8Spec.*;
import static org.junit.Assert.*;
import static com.github.mizosoft.methanol.MutableRequest.*;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static java.lang.String.format;

// These tests require podman-compose up and /etc/hosts mappings defined

@DefinedOrder
@RunWith(J8SpecRunner.class)
public class A4ServiceTest {

  private static Logger log;
  private static A4Context ctx = new A4Context();
  private static final Gson gson = new Gson();
  private static final SSLContext trustAllCtx = SSLCertificates.trustAllContext();
  private static final Methanol apiClient = Methanol.newBuilder()
      .baseUri("http://localhost:7070")
      .build();

  private static final String
      testConfigId = "test-config-00",
      tempConfigId = "temp-test-config";

  private static final A4Config tempConfig = new A4Config()
      .id(tempConfigId)
      .description("Test runtime configuration")
      .server(
          new A4Server()
              .id("temp-http-00")
              .addr(new A4Sock().host("127.0.0.1").port(8080))
              .match(
                  new A4Match().pool(new A4Pool().hosts(
                      new A4Backend().addr(new A4Sock().host("nowhere.localhost").port(10022))
                  ))
              )
      );

  public static String doRequest(Methanol m, MutableRequest req, long sleepMs) throws IOException, InterruptedException {
    var res = m.send(req, ofString());
    Thread.sleep(sleepMs);
    return res.body();
  }

  public static void doGet(String url, int count) throws IOException, InterruptedException {
    var req = GET(url);
    var m = Methanol.newBuilder().sslContext(trustAllCtx).build();
    for (int i = 0; i < count; i++) {
      log.info(doRequest(m, req, 500));
    }
  }

  public static String doGet(Methanol m, String url) throws IOException, InterruptedException {
    var req = GET(url);
    return doRequest(m, req, 0);
  }

  public static String doPost(Methanol m, String url, Object body) throws IOException, InterruptedException {
    var json = gson.toJson(body);
    var req = POST(url, BodyPublishers.ofString(json));
    return doRequest(m, req, 0);
  }

  public static void doUdpGet(String msg, int count, long sleepMs) throws IOException, InterruptedException {
    try (var udpClient = new A4UdpClient()) {
      for (int i = 0; i < count; i++) {
        log.info("Received reply: [{}]", udpClient.sendEcho(msg));
        Thread.sleep(sleepMs);
      }
    }
  }

  static {
    it("Initializes the Load Balancer context", () -> {
      var fl = A4Flags.from(new String[] {
          flagOf(kLogLevel, "debug"),
          flagOf(kConfig, "./src/test/resources")
      });
      ctx.init(fl);
      log = LoggerFactory.getLogger(A4ServiceTest.class);
      Thread.sleep(5000); // Integer.MAX_VALUE
    });

    it("Attempts to add an invalid configuration", () -> {
      var cfg = new A4Config();
      var res = doPost(apiClient, A4Route.apiV1Config, cfg);
      log.info(res);
      assertNotNull(res);
      assertFalse(res.isEmpty());
    });

    it("Adds a new configuration", () -> {
      log.info(tempConfig.toString());
      var query = format("%s?configId=%s", A4Route.apiV1Config, tempConfig.id);
      var res = doPost(apiClient, query, tempConfig);
      log.info(res);
      assertNotNull(res);
      assertEquals("[]", res);
    });

    it("Opens the new configuration", () -> {
      doGet(apiClient, format("%s?%s=%s", apiV1ConfigSelect, pConfigId, tempConfigId));
      // Thread.sleep(Integer.MAX_VALUE);
    });

    it("Closes the new configuration", () -> doGet(apiClient, apiV1ConfigSelect));

    it("Opens the initial configuration", () -> {
      doGet(apiClient, format("%s?%s=%s", apiV1ConfigSelect, pConfigId, testConfigId));
      if (ctx.service.instance != null) {
        for (var srv : ctx.service.instance.config.servers) {
          for (var m : A4Configs.allMatchesOf(srv)) {
            log.info(m.toString());
          }
        }
      }
      Thread.sleep(10000);
    });

    it("Deletes the new configuration", () -> {
      // Thread.sleep(Integer.MAX_VALUE);
      var req = MutableRequest.create()
          .uri(format("%s?%s=%s", apiV1Config, pConfigId, tempConfigId))
          .method("DELETE", BodyPublishers.ofString(""));
      doRequest(apiClient, req, 0);
    });

    it("Requests the active configuration", () -> log.info(doGet(apiClient, A4Route.apiV1Config)));

    it("Requests all configurations", () -> log.info(doGet(apiClient, A4Route.apiV1ConfigList)));

    it("Sends UDP requests", () -> {
      var msg = "Hello UDP";
      doUdpGet(msg, 3, 4000);
      doUdpGet(msg, 3, 400);
    });

    it("Sends plain HTTP requests", () -> {
      doGet("http://127.0.0.1:8080", 20);
      doGet("http://127.0.0.1:8090", 20);
      doGet("https://momo.localhost:8443", 20);
      doGet("https://sdr.localhost:8443", 20);
    });

    it("Sends a curl request", () -> {
      var res = ProcBuilder.run("curl", "--verbose", "http://127.0.0.1:8080");
      log.info(res);
    });

    // TODO Remaining tests
    //   - Retrieve performance metrics

    it("Closes the initial configuration", () -> doGet(apiClient, apiV1ConfigSelect));

    it("Stops the Load Balancer context", () -> ctx.close());
  }

}
