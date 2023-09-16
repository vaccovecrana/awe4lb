package io.vacco.a4lb;

import com.google.gson.Gson;
import io.vacco.a4lb.tcp.*;
import io.vacco.a4lb.util.A4Configs;
import io.vacco.shax.logging.ShOption;
import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.ArrayList;

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
    it("Forwards socket data", () -> {
      // https://gobetween.io/documentation.html#Balancing
      // InetSocketAddress dummy = new InetSocketAddress("websdr.ewi.utwente.nl", 8901);
      // InetSocketAddress dummy = new InetSocketAddress("172.16.3.233", 9096);

      var cfg = A4Configs.loadFrom(A4ValidTest.class.getResource("/config.json"), new Gson());
      var srvE = new ArrayList<>(cfg.servers.entrySet());
      var srvN = srvE.get(0);
      var srv = new A4TcpSrv(A4Io.newSelector(), srvN.getKey(), srvN.getValue());
      while (true) {
        srv.update();
      }
    });
  }

}
