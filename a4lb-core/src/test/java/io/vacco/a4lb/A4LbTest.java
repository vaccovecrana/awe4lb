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
      var cfg = A4Configs.loadFrom(A4ValidTest.class.getResource("/config.json"), new Gson());
      var srvE = new ArrayList<>(cfg.servers.entrySet());
      var srvN = srvE.get(1);
      var srv = new A4TcpSrv(A4Io.newSelector(), srvN.getKey(), srvN.getValue());
      while (true) {
        srv.update();
      }
    });
  }
}
