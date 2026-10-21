package io.vacco.a4lb;

import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.util.A4MatchOps;
import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;

import java.util.List;

import static j8spec.J8Spec.it;
import static org.junit.Assert.*;

@DefinedOrder
@RunWith(J8SpecRunner.class)
public class A4MatchOpsTest {
  static {
    it("Evaluates string operations", () -> {
      var equalsOp = new A4StringOp().equals("test");
      var startsWithOp = new A4StringOp().startsWith("te");
      var endsWithOp = new A4StringOp().endsWith("st");
      assertTrue(A4MatchOps.eval(equalsOp, "test"));
      assertFalse(A4MatchOps.eval(equalsOp, "fail"));
      assertTrue(A4MatchOps.eval(startsWithOp, "test"));
      assertFalse(A4MatchOps.eval(startsWithOp, "fail"));
      assertTrue(A4MatchOps.eval(endsWithOp, "test"));
      assertFalse(A4MatchOps.eval(endsWithOp, "fail"));
    });

    it("Evaluates match operations with sni and host", () -> {
      var sniOp = new A4MatchOp().sni(new A4StringOp().equals("example.com"));
      var hostOp = new A4MatchOp().host(new A4StringOp().endsWith("tname"));

      assertTrue(A4MatchOps.eval(sniOp, "example.com", "host"));
      assertFalse(A4MatchOps.eval(sniOp, "fail.com", "host"));

      assertTrue(A4MatchOps.eval(hostOp, "example.com", "hostname"));
      assertFalse(A4MatchOps.eval(hostOp, "example.com", "failname"));
    });

    it("Evaluates match rules to find the correct pool", () -> {
      var op0 = new A4MatchOp().sni(new A4StringOp().equals("example.com"));
      var op1 = new A4MatchOp().host(new A4StringOp().endsWith("host"));
      var op2 = new A4MatchOp()
        .host(new A4StringOp().startsWith("127.0"))
        .sni(new A4StringOp().equals("stonk.me"));

      var pool0 = new A4Pool().hosts(new A4Backend().addr(new A4Sock().host("host0").port(8080)).weight(1).priority(0));
      var pool1 = new A4Pool().hosts(new A4Backend().addr(new A4Sock().host("host1").port(8443)).weight(4).priority(1));
      var pool2 = new A4Pool().hosts(new A4Backend().addr(new A4Sock().host("host2").port(9999)));

      var rules = List.of(
        new A4Match().op(op0).pool(pool0),
        new A4Match().op(op1).pool(pool1),
        new A4Match().op(op2).pool(pool2)
      );

      var result = A4MatchOps.eval("example.com", "localhost", rules);
      assertTrue(result.isPresent());
      assertEquals(result.get().pool, pool0);

      result = A4MatchOps.eval("fail.com", "localhost", rules);
      assertTrue(result.isPresent());
      assertEquals(result.get().pool, pool1);

      result = A4MatchOps.eval("fail.com", "failname", rules);
      assertFalse(result.isPresent());

      result = A4MatchOps.eval("stonk.me", "127.0.0.1", rules);
      assertTrue(result.isPresent());
      assertEquals(result.get().pool, pool2);
    });

    it("Matches case-insensitively without locale surprises", () -> {
      var op = new A4StringOp().equals("istanbul");
      assertTrue(A4MatchOps.eval(op, "ISTANBUL"));
      assertTrue(A4MatchOps.eval(op, "Istanbul"));
      assertFalse(A4MatchOps.eval(op, null));
      assertFalse(A4MatchOps.eval(null, "istanbul"));
    });

    it("Requires both host and SNI operations when both are configured", () -> {
      var op = new A4MatchOp()
        .host(new A4StringOp().startsWith("10."))
        .sni(new A4StringOp().endsWith(".example.com"));
      assertTrue(A4MatchOps.eval(op, "api.example.com", "10.0.0.1"));
      assertFalse(A4MatchOps.eval(op, "api.example.com", "192.168.0.1"));
      assertFalse(A4MatchOps.eval(op, "other.test", "10.0.0.1"));
    });

    it("Treats a match without operations as a catch-all", () -> {
      var catchAll = new A4Match().pool(new A4Pool());
      var specific = new A4Match().op(new A4MatchOp().host(new A4StringOp().equals("later"))).pool(new A4Pool());
      var result = A4MatchOps.eval("anything", "anything", List.of(catchAll, specific));
      assertTrue(result.isPresent());
      assertEquals(catchAll, result.get());
    });
  }
}
