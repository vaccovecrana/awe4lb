package io.vacco.a4lb;

import io.vacco.shax.logging.ShOption;
import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;
import java.net.InetSocketAddress;

import static j8spec.J8Spec.*;

@DefinedOrder
@RunWith(J8SpecRunner.class)
public class A4LbTest {

  static {
    ShOption.setSysProp(ShOption.IO_VACCO_SHAX_DEVMODE, "true");
    ShOption.setSysProp(ShOption.IO_VACCO_SHAX_PRETTYPRINT, "true");
    ShOption.setSysProp(ShOption.IO_VACCO_SHAX_LOGLEVEL, "trace");
  }

  static {
    it("Forwards socket data", () -> {
      var srv = new A4TcpSrv(A4Io.osSelector(), new InetSocketAddress("0.0.0.0", 8080));
      while (true) {
        srv.update();
      }
    });
  }
}
