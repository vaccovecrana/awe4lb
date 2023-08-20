package io.vacco.a4lb;

import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.tcp.*;
import io.vacco.shax.logging.ShOption;
import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;
import org.slf4j.bridge.SLF4JBridgeHandler;
import java.net.InetSocketAddress;

import static j8spec.J8Spec.*;

@DefinedOrder
@RunWith(J8SpecRunner.class)
public class A4LbTest {

  static {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
    ShOption.setSysProp(ShOption.IO_VACCO_SHAX_DEVMODE, "true");
    ShOption.setSysProp(ShOption.IO_VACCO_SHAX_PRETTYPRINT, "true");
    ShOption.setSysProp(ShOption.IO_VACCO_SHAX_LOGLEVEL, "trace");
  }

  static {
    it("Validates service definitions", () -> {
      var a4s = new A4Service()
          .withHost("0.0.0.0").withPort(75000)
          .withHealthCheck(
              new A4HealthCheck()
                  .withIntervalMs(3000)
                  .withTimeoutMs(5000)
          );
      var constraints = A4Valid.A4ServiceVld.validate(a4s);
      for (var cnt : constraints) {
        System.out.println(cnt.message());
      }
    });
    it("Forwards socket data", () -> {
      var srv = new A4TcpSrv(A4Io.osSelector(), new InetSocketAddress("0.0.0.0", 8080));
      while (true) {
        srv.update();
      }
    });
  }
}
