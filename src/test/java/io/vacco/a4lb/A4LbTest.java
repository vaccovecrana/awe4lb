package io.vacco.a4lb;

import com.google.gson.Gson;
import io.vacco.shax.logging.ShOption;
import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;

import static j8spec.J8Spec.*;

@DefinedOrder
@RunWith(J8SpecRunner.class)
public class A4LbTest {

  static {
    ShOption.setSysProp(ShOption.IO_VACCO_SHAX_DEVMODE, "true");
    ShOption.setSysProp(ShOption.IO_VACCO_SHAX_PRETTYPRINT, "true");
    ShOption.setSysProp(ShOption.IO_VACCO_SHAX_LOGLEVEL, "debug");
  }

  static {
    it("Forwards socket data", () -> {
      var g = new Gson();
      var cfgUrl = A4LbTest.class.getResource("/config.json");
      var a4lb = A4LbMain.init(cfgUrl, g);
      var a4Api = new A4Api(g).start();
      Thread.sleep(15000);
      a4lb.stop();
      a4Api.stop();
    });
  }

}
