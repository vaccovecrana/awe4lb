package io.vacco.a4lb;

import io.vacco.a4lb.util.A4Base36;
import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;

import static j8spec.J8Spec.it;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@DefinedOrder
@RunWith(J8SpecRunner.class)
public class A4Base36Test {
  static {
    it("Produces stable four-character identifiers", () -> {
      var a = A4Base36.hash4("127.0.0.1:8080");
      var b = A4Base36.hash4("127.0.0.1:8080");
      assertEquals(4, a.length());
      assertEquals(a, b);
      assertTrue(a.matches("[0-9a-z]{4}"));
    });

    it("Handles hash codes that overflow Math.abs", () -> {
      var id = A4Base36.hash4("polygenelubricants");
      assertEquals(4, id.length());
      assertTrue(id.matches("[0-9a-z]{4}"));
    });
  }
}
