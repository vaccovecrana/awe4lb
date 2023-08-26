package io.vacco.a4lb;

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

  /*
    private static void getActiveProtocols() throws NoSuchAlgorithmException {
    var eng = SSLContext.getDefault().createSSLEngine();
    System.out.println(Arrays.toString(eng.getEnabledProtocols()));
    System.out.println(Arrays.toString(eng.getEnabledCipherSuites()));
  } */

  static {
    it("Forwards socket data", () -> {
      var srv = new A4TcpSrv(A4Io.osSelector(), new InetSocketAddress("0.0.0.0", 8080));
      while (true) {
        srv.update();
      }
    });
  }
}
