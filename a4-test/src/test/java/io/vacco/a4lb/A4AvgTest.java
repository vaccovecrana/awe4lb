package io.vacco.a4lb;

import io.vacco.a4lb.cfg.A4Avg;
import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;

import static j8spec.J8Spec.*;
import static org.junit.Assert.*;

@DefinedOrder
@RunWith(J8SpecRunner.class)
public class A4AvgTest {
  static {
    it("Computes a rolling average over the configured window", () -> {
      var avg = new A4Avg().init(3);
      avg.update(1);
      avg.update(2);
      avg.update(3);
      assertEquals(2.0f, avg.get(), 0.0001);
      avg.update(4);
      assertEquals(3.0f, avg.get(), 0.0001);
    });

    it("Resets all state", () -> {
      var avg = new A4Avg().init(2);
      avg.update(10);
      avg.reset();
      assertEquals(0.0f, avg.get(), 0.0001);
      avg.update(4);
      assertEquals(4.0f, avg.get(), 0.0001);
    });

    it("Rejects invalid window sizes and uninitialized updates", () -> {
      assertThrows(IllegalArgumentException.class, () -> new A4Avg().init(0));
      assertThrows(IllegalStateException.class, () -> new A4Avg().update(1));
    });
  }
}
