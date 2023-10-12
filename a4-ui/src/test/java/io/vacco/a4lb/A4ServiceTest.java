package io.vacco.a4lb;

import com.google.gson.Gson;
import io.vacco.a4lb.impl.A4Lb;
import io.vacco.a4lb.util.*;
import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;

import static j8spec.J8Spec.*;

@DefinedOrder
@RunWith(J8SpecRunner.class)
public class A4ServiceTest {

  static {
    it("Forwards socket data", () -> {
      var fl = A4Flags.from(new String[]{
          "--log-level=debug",
          "--config=./src/test/resources"
      });
      var svc = new A4Service().init(fl);

      var g = new Gson();
      svc.config = A4Configs.loadFrom(A4ServiceTest.class.getResource("/config.json"), g);
      svc.instance = new A4Lb(svc.config, g).open();
      Thread.sleep(35000);

      svc.close();
    });
  }

}