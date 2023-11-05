package io.vacco.a4lb;

import com.github.mizosoft.methanol.*;
import com.google.gson.Gson;
import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.niossl.SSLCertificates;
import io.vacco.a4lb.util.*;
import io.vacco.a4lb.web.A4Route;
import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.buildobjects.process.ProcBuilder;
import org.junit.runner.RunWith;
import org.slf4j.*;
import javax.net.ssl.SSLContext;
import java.io.IOException;

import static io.vacco.a4lb.util.A4Flags.*;
import static j8spec.J8Spec.*;
import static org.junit.Assert.*;
import static com.github.mizosoft.methanol.MutableRequest.*;
import static java.net.http.HttpResponse.BodyHandlers.ofString;

// These tests require podman-compose up and /etc/hosts mappings defined

@DefinedOrder
@RunWith(J8SpecRunner.class)
public class A4ServiceTest {

  private static Logger log;
  private static A4Service svc;
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
              .id("http-test-00")
              .addr(new A4Sock().host("127.0.0.1").port(8080))
              .match(
                  new A4Match().pool(new A4Pool().hosts(
                      new A4Backend().addr(new A4Sock().host("somewhere.io").port(10022))
                  ))
              )
      );

  public static String doRequest(Methanol m, MutableRequest req, long sleepMs) throws IOException, InterruptedException {
    var res = m.send(req, ofString());
    Thread.sleep(sleepMs);
    return res.body();
  }

  public static void repeatRequest(Methanol m, MutableRequest req, long sleepMs, int count) throws IOException, InterruptedException {
    for (int i = 0; i < count; i++) {
      log.info(doRequest(m, req, sleepMs));
    }
  }

  public static void doGet(String url, int count) throws IOException, InterruptedException {
    var req = GET(url);
    repeatRequest(Methanol.newBuilder().sslContext(trustAllCtx).build(), req, count, 500);
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
    it("Initializes the Load Balancer Service/UI", () -> {
      var fl = A4Flags.from(new String[] {
          flagOf(kLogLevel, "trace"),
          flagOf(kConfig, "./src/test/resources")
      });
      svc = new A4Service().init(fl);
      log = LoggerFactory.getLogger(A4ServiceTest.class);
      Thread.sleep(5000); // Integer.MAX_VALUE
    });

    it("Sends a curl request", () -> {
      var res = ProcBuilder.run("curl", "--verbose", "http://127.0.0.1:8080");
      log.info(res);
    });

    it("Loads the active configuration", () -> doGet(apiClient, A4Route.apiV1Config));
    it("Loads all configurations", () -> doGet(apiClient, A4Route.apiV1ConfigList));

    it("Attempts to add an invalid configuration", () -> {
      var cfg = new A4Config();
      var res = doPost(apiClient, A4Route.apiV1Config, cfg);
      log.info(res);
      assertNotNull(res);
      assertFalse(res.isEmpty());
    });
    it("Adds a new configuration", () -> {
      var res = doPost(apiClient, A4Route.apiV1Config, tempConfig);
      log.info(res);
      assertNotNull(res);
      assertEquals("[]", res);
    });
    it("Opens the new configuration", () -> svc.setActive(tempConfigId));
    it("Closes the new configuration and restarts the original active configuration", () -> svc.setActive(testConfigId));
    it("Deletes the new configuration", () -> {
      create("lol")
    });

    it("Sends UP requests", () -> {
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

    // TODO Remaining tests
    //   - Retrieve performance metrics

    it("Stops the Load Balancer Service/UI", () -> svc.close());
  }

}
