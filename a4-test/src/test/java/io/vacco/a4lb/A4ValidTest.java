package io.vacco.a4lb;

import com.google.gson.Gson;
import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.util.A4Configs;
import io.vacco.a4lb.util.A4Valid;
import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;

import static j8spec.J8Spec.it;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@DefinedOrder
@RunWith(J8SpecRunner.class)
public class A4ValidTest {

  private static A4Tls tls(String path) {
    var tls = new A4Tls();
    tls.certPath = path;
    tls.keyPath = path;
    return tls;
  }

  private static A4Config tlsMatches(A4ServerTls serverTls, boolean firstTls, boolean secondTls) {
    var first = new A4Match()
      .op(new A4MatchOp().sni(new A4StringOp().equals("gopher.io")))
      .pool(new A4Pool().hosts(
        new A4Backend().addr(new A4Sock().host("127.0.0.1").port(4000))
      ));
    var second = new A4Match()
      .op(new A4MatchOp().sni(new A4StringOp().equals("git.momo.net")))
      .pool(new A4Pool().hosts(
        new A4Backend().addr(new A4Sock().host("127.0.0.1").port(4001))
      ));
    if (firstTls) {
      first.tls = tls("./build.gradle.kts");
    }
    if (secondTls) {
      second.tls = tls("./build.gradle.kts");
    }
    var server = new A4Server()
      .id("tls-int")
      .addr(new A4Sock().host("0.0.0.0").port(8443))
      .match(first, second);
    server.tls = serverTls;
    return new A4Config()
      .id("tls-validation")
      .description("TLS validation")
      .server(server);
  }

  static {
    it("Validates a configuration", () -> {
      var a4Cfg = new A4Config()
        .server(new A4Server()
          .id("momo-tls")
          .addr(new A4Sock().host("0.0.0.0").port(75000))
          .tls(
            new A4ServerTls()
              .protocols("momo-protocol")
              .ciphers("momo-cipher-cha-cha-cha")
          )
          .match(
            new A4Match()
              .op(
                new A4MatchOp()
                  .sni(new A4StringOp().equals("ci.gopher.io"))
                  .host(new A4StringOp().endsWith("momo"))
              ).pool(
                new A4Pool().hosts(
                  new A4Backend()
                    .addr(new A4Sock().host("tct00.gopher.io").port(8080))
                    .weight(1)
                    .priority(0)
                )
              ).healthCheck(
                new A4HealthCheck().intervalMs(3000).timeoutMs(5000)
              ),
            new A4Match()
              .op(new A4MatchOp().host(new A4StringOp().startsWith("172.16")))
              .pool(new A4Pool())
              .discover(new A4Disc())
          )
        );
      var constraints = A4Valid.validate(a4Cfg);
      System.out.println(A4Valid.validationsOf(constraints));
    });
    it("Validates a JSON configuration", () -> {
      System.out.println("============================");
      var cfg = A4Configs.loadFromOrFail(A4ValidTest.class.getResource("/test-config-00.json"), new Gson());
      var constraints = A4Valid.validate(cfg);
      for (var cnt : constraints) {
        System.out.println(cnt.message());
      }
    });
    it("Validates a K8S discovery configuration", () -> {
      var k8s = new A4DiscK8s();
      k8s.port = 8080;
      k8s.service = "momo";
      k8s.namespace = "test";
      k8s.tokenPath = "./build.gradle.kts";
      k8s.apiUri = "https://k8s.example.com:6443";
      System.out.println(A4Valid.validationsOf(A4Valid.validate(k8s)));
    });

    it("Rejects match-level TLS without a server-level TLS block", () -> {
      assertFalse(A4Valid.validate(tlsMatches(null, true, false)).isEmpty());
    });

    it("Allows an empty server TLS block when every match has a certificate", () -> {
      assertTrue(A4Valid.validate(tlsMatches(new A4ServerTls(), true, true)).isEmpty());
    });

    it("Allows a server base certificate to cover matches without TLS blocks", () -> {
      var serverTls = new A4ServerTls();
      serverTls.base = tls("./build.gradle.kts");
      assertTrue(A4Valid.validate(tlsMatches(serverTls, true, false)).isEmpty());
    });

    it("Rejects an empty server base TLS configuration", () -> {
      var serverTls = new A4ServerTls();
      serverTls.base = new A4Tls();
      assertFalse(A4Valid.validate(tlsMatches(serverTls, true, false)).isEmpty());
    });
  }
}
