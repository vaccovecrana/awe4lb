package io.vacco.a4lb;

import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.sel.*;
import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;

import java.util.List;

import static j8spec.J8Spec.*;
import static org.junit.Assert.*;

@DefinedOrder
@RunWith(J8SpecRunner.class)
public class A4SelectorsTest {

  private static A4Backend up(String host, int port) {
    return new A4Backend().addr(new A4Sock().host(host).port(port)).state(A4BackendState.Up);
  }

  static {
    it("Cycles through round-robin backends", () -> {
      var b0 = up("b0", 1);
      var b1 = up("b1", 2);
      var pool = new A4Pool().hosts(b0, b1);
      pool.type = A4PoolType.roundRobin;
      var sel = new A4Selector(List.of());
      assertEquals(b0, sel.select(pool, 0));
      assertEquals(b1, sel.select(pool, 0));
      assertEquals(b0, sel.select(pool, 0));
    });

    it("Selects the last IP-hash bucket and handles a single backend", () -> {
      var b0 = up("b0", 1);
      var b1 = up("b1", 2);
      var b2 = up("b2", 3);
      var pool = new A4Pool().hosts(b0, b1, b2);
      pool.type = A4PoolType.ipHash;
      var sel = new A4Selector(List.of());
      assertEquals(b2, sel.select(pool, 2));
      assertEquals(b0, sel.select(pool, 3));

      var single = new A4Pool().hosts(b0);
      single.type = A4PoolType.ipHash;
      assertEquals(b0, sel.select(single, 12345));
    });

    it("Picks the backend with the fewest active connections", () -> {
      var b0 = up("b0", 1);
      var b1 = up("b1", 2);
      var pool = new A4Pool().hosts(b0, b1);
      pool.type = A4PoolType.leastConn;
      var sel = new A4Selector(List.of());

      assertEquals(b0, sel.select(pool, 0));
      sel.contextOf(b0).trackConn(true);
      assertEquals(b1, sel.select(pool, 0));
      sel.contextOf(b1).trackConn(true);
      sel.contextOf(b1).trackConn(true);
      assertEquals(b0, sel.select(pool, 0));
    });

    it("Honours the highest-priority weighted group", () -> {
      var lowPriority = up("b0", 1);
      lowPriority.weight(1);
      lowPriority.priority(0);
      var highPriority = up("b1", 2);
      highPriority.weight(100);
      highPriority.priority(1);
      var pool = new A4Pool().hosts(lowPriority, highPriority);
      pool.type = A4PoolType.weight;
      var sel = new A4Selector(List.of());
      for (int i = 0; i < 50; i++) {
        assertEquals(lowPriority, sel.select(pool, 0));
      }
    });

    it("Rejects selection when no backend is available", () -> {
      var pool = new A4Pool().hosts();
      var sel = new A4Selector(List.of());
      var ctx = new A4PoolContext();
      assertThrows(IllegalStateException.class, () -> A4SelRandom.select(pool, ctx));
      assertThrows(IllegalStateException.class, () -> A4SelRRobin.select(pool, ctx));
      assertThrows(IllegalStateException.class, () -> A4SelIpHash.select(pool, 0));
      assertThrows(IllegalStateException.class, () -> A4SelWeight.select(pool, ctx));
      assertThrows(IllegalStateException.class, () -> A4SelLConn.select(pool, new java.util.HashMap<>()));
      assertThrows(IllegalStateException.class, () -> sel.select(pool, 0));
    });
  }
}
