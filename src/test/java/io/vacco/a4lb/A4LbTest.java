package io.vacco.a4lb;

import io.vacco.a4lb.util.A4Flags;
import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;

import static j8spec.J8Spec.*;

@DefinedOrder
@RunWith(J8SpecRunner.class)
public class A4LbTest {

  static {
    it("Forwards socket data", () -> {
      var fl = A4Flags.from(new String[]{
          "--log-level=debug",
          "--config=./src/test/resources"
      });
      var svc = new A4Service().init(fl);
      Thread.sleep(15000);
      svc.instance.stop();
    });
  }

}
