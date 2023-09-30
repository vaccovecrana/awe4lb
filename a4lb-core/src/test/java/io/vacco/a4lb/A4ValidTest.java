package io.vacco.a4lb;

import com.google.gson.Gson;
import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.util.A4Configs;
import io.vacco.a4lb.util.A4Valid;
import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;

import static j8spec.J8Spec.*;

@DefinedOrder
@RunWith(J8SpecRunner.class)
public class A4ValidTest {
  static {
    it("Validates a configuration", () -> {
      var a4Cfg = new A4Config()
          .server(new A4Server()
              .id("momo-tls")
              .addr(new A4Sock().host("0.0.0.0").port(75000))
              .tls(
                  new A4Tls()
                      .certPath("/etc/momo/momo.pem")
                      .keyPath("/etc/momo/momo-key.pem")
                      .protocols(" ")
                      .ciphers("momo-cipher")
              )
              .match(
                  new A4Match()
                      .and(new A4MatchOp().sni(new A4StringOp().equals("ci.gopher.io")))
                      .or(new A4MatchOp().host(new A4StringOp().contains("momo")))
                      .pool(
                          new A4Pool().hosts(
                              new A4Backend().addr(
                                  new A4Sock().host("tct00.gopher.io").port(8080)
                              ).weight(1).priority(0)
                          )
                      ).healthCheck(
                          new A4HealthCheck().intervalMs(3000).timeoutMs(5000)
                      ),
                  new A4Match()
                      .and(new A4MatchOp().host(new A4StringOp().startsWith("172.16")))
                      .pool(new A4Pool())
                      .discover(new A4Disc())
              )
          );
      var constraints = A4Valid.A4ConfigVld.validate(a4Cfg);
      for (var cnt : constraints) {
        System.out.println(cnt.message());
      }
    });
    it("Validates a JSON configuration", () -> {
      System.out.println("============================");
      var cfg = A4Configs.loadFrom(A4ValidTest.class.getResource("/config.json"), new Gson());
      var constraints = A4Valid.A4ConfigVld.validate(cfg);
      for (var cnt : constraints) {
        System.out.println(cnt.message());
      }
      for (var srv : cfg.servers) {
        for (var match : srv.match) {
          System.out.printf("%x%n", match.pool.hashCode());
        }
      }
    });
  }
}
