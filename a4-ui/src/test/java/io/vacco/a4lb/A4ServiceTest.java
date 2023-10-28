package io.vacco.a4lb;

import io.vacco.a4lb.util.*;
import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;

import static io.vacco.a4lb.util.A4Flags.*;
import static j8spec.J8Spec.*;

@DefinedOrder
@RunWith(J8SpecRunner.class)
public class A4ServiceTest {

  static {
    it("Forwards socket data", () -> {
      var fl = A4Flags.from(new String[] {
          flagOf(kLogLevel, "debug"),
          flagOf(kConfig, "./src/test/resources")
      });
      var svc = new A4Service().init(fl);
      Thread.sleep(Integer.MAX_VALUE);
      svc.close();
    });
  }

}
