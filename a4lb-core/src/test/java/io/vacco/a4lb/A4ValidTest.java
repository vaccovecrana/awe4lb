package io.vacco.a4lb;

import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.util.A4Valid;
import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;

import static j8spec.J8Spec.*;

@DefinedOrder
@RunWith(J8SpecRunner.class)
public class A4ValidTest {
  static {
    it("Validates a service definition", () -> {
      var a4s = new A4Service()
          .addr(new A4Sock().host("0.0.0.0").port(75000))
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
                  )
          )
          .healthCheck(
              new A4HealthCheck()
                  .intervalMs(3000)
                  .timeoutMs(5000)
          );
      var constraints = A4Valid.A4ServiceVld.validate(a4s);
      for (var cnt : constraints) {
        System.out.println(cnt.message());
      }
    });

  }
}
