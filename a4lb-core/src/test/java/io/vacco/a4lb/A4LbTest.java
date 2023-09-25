package io.vacco.a4lb;

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
      // https://gobetween.io/documentation.html#Balancing
      // InetSocketAddress dummy = new InetSocketAddress("172.16.3.233", 9096);
      A4Lb.main(new String[] {"./src/test/resources/config.json"});
    });
  }

}
