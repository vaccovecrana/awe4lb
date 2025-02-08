package io.vacco.a4lb;

import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.util.A4MatchOps;
import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Optional;

import static j8spec.J8Spec.*;
import static org.junit.Assert.*;

@DefinedOrder
@RunWith(J8SpecRunner.class)
public class A4MatchOpsTest {
  static {
    it("Evaluates string operations", () -> {
      var equalsOp = new A4StringOp().equals("test");
      var containsOp = new A4StringOp().contains("es");
      var startsWithOp = new A4StringOp().startsWith("te");
      var endsWithOp = new A4StringOp().endsWith("st");

      assertTrue(A4MatchOps.eval(equalsOp, "test"));
      assertFalse(A4MatchOps.eval(equalsOp, "fail"));

      assertTrue(A4MatchOps.eval(containsOp, "test"));
      assertFalse(A4MatchOps.eval(containsOp, "fail"));

      assertTrue(A4MatchOps.eval(startsWithOp, "test"));
      assertFalse(A4MatchOps.eval(startsWithOp, "fail"));

      assertTrue(A4MatchOps.eval(endsWithOp, "test"));
      assertFalse(A4MatchOps.eval(endsWithOp, "fail"));
    });

    it("Evaluates match operations with sni and host", () -> {
      var sniOp = new A4MatchOp().sni(new A4StringOp().equals("example.com"));
      var hostOp = new A4MatchOp().host(new A4StringOp().contains("host"));

      assertTrue(A4MatchOps.eval(sniOp, "example.com", "host"));
      assertFalse(A4MatchOps.eval(sniOp, "fail.com", "host"));

      assertTrue(A4MatchOps.eval(hostOp, "example.com", "hostname"));
      assertFalse(A4MatchOps.eval(hostOp, "example.com", "failname"));
    });

    it("Evaluates AND operations on match operations", () -> {
      var sniOp = new A4MatchOp().sni(new A4StringOp().equals("example.com"));
      var hostOp = new A4MatchOp().host(new A4StringOp().contains("host"));

      assertTrue(A4MatchOps.evalAnd("example.com", "hostname", sniOp, hostOp));
      assertFalse(A4MatchOps.evalAnd("example.com", "failname", sniOp, hostOp));
    });

    it("Evaluates OR operations on match operations", () -> {
      var sniOp = new A4MatchOp().sni(new A4StringOp().equals("example.com"));
      var hostOp = new A4MatchOp().host(new A4StringOp().contains("host"));

      assertTrue(A4MatchOps.evalOr("example.com", "hostname", sniOp, hostOp));
      assertTrue(A4MatchOps.evalOr("example.com", "failname", sniOp, hostOp));
      assertFalse(A4MatchOps.evalOr("fail.com", "failname", sniOp, hostOp));
    });

    it("Evaluates match rules to find the correct pool", () -> {
      var sniOp = new A4MatchOp().sni(new A4StringOp().equals("example.com"));
      var hostOp = new A4MatchOp().host(new A4StringOp().contains("host"));
      var pool = new A4Pool().hosts(new A4Backend().addr(new A4Sock().host("host1").port(8080)).weight(1).priority(0));
      var rules = List.of(
        new A4Match().and(sniOp).pool(pool),
        new A4Match().or(hostOp).pool(pool)
      );

      var result = A4MatchOps.eval("example.com", "hostname", rules);
      assertTrue(result.isPresent());
      assertEquals(result.get().pool, pool);

      result = A4MatchOps.eval("fail.com", "hostname", rules);
      assertTrue(result.isPresent());
      assertEquals(result.get().pool, pool);

      result = A4MatchOps.eval("fail.com", "failname", rules);
      assertFalse(result.isPresent());
    });
  }
}
