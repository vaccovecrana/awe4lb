package io.vacco.a4lb;

import com.github.mizosoft.methanol.Methanol;
import com.google.gson.Gson;
import io.vacco.a4lb.niossl.SSLCertificates;
import io.vacco.a4lb.util.*;
import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;
import org.slf4j.*;
import javax.net.ssl.SSLContext;
import java.io.IOException;

import static io.vacco.a4lb.util.A4Flags.*;
import static j8spec.J8Spec.*;
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

  public static void doGet(String url, int count) throws IOException, InterruptedException {
    var req = GET(url);
    for (int i = 0; i < count; i++) {
      var client = Methanol.newBuilder().sslContext(trustAllCtx).build();
      var res = client.send(req, ofString());
      log.info(res.body());
      Thread.sleep(500);
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
      Thread.sleep(5000);
    });
    it("Sends plain HTTP requests", () -> {
      doGet("http://127.0.0.1:8080", 20);
      doGet("http://127.0.0.1:8090", 20);
      doGet("https://momo.localhost:8443", 20);
      doGet("https://sdr.localhost:8443", 20);
    });

    // TODO Remaining tests
    //   - Send UDP requests
    //   - Query all API/UI endpoints
    //   - Register new configuration
    //   - Activate new configuration
    //   - Stop new configuration
    //   - Delete new configuration
    //   - Register an invalid configuration
    //   - Retrieve performance metrics

    it("Stops the Load Balancer Service/UI", () -> svc.close());
  }

}
