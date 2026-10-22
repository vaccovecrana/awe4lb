package io.vacco.a4lb;

import io.vacco.a4lb.util.A4TtlMap;
import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicInteger;

import static j8spec.J8Spec.it;
import static org.junit.Assert.*;

@DefinedOrder
@RunWith(J8SpecRunner.class)
public class A4TtlMapTest {
  static {
    it("Evicts the oldest entry when the map is full", () -> {
      var map = new A4TtlMap<String, String>(2, v -> {
      });
      map.put("a", "1", -1);
      map.put("b", "2", -1);
      map.put("c", "3", -1);
      assertEquals(2, map.size());
      assertNull(map.get("a"));
      assertEquals("2", map.get("b"));
      assertEquals("3", map.get("c"));
    });

    it("Expires entries and notifies the callback exactly once", () -> {
      var expired = new AtomicInteger();
      var map = new A4TtlMap<String, String>(4, v -> expired.incrementAndGet());
      map.put("a", "1", 1);
      Thread.sleep(10);
      assertNull(map.get("a"));
      assertNull(map.get("a"));
      assertEquals(1, expired.get());
      assertEquals(0, map.size());
    });

    it("Refreshes an existing entry without duplicating it", () -> {
      var map = new A4TtlMap<String, String>(2, v -> {
      });
      map.put("a", "1", -1);
      map.put("a", "2", -1);
      assertEquals(1, map.size());
      assertEquals("2", map.get("a"));
      map.put("b", "3", -1);
      assertEquals(2, map.size());
      assertEquals("2", map.get("a"));
    });

    it("Extends the expiry of an existing entry", () -> {
      var map = new A4TtlMap<String, String>(2, v -> {
      });
      map.put("a", "1", 500);
      Thread.sleep(100);
      assertTrue(map.refresh("a", 1000));
      Thread.sleep(600);
      assertEquals("1", map.get("a"));
      assertEquals(1, map.size());
    });

    it("Rejects refreshes for missing or expired entries", () -> {
      var expired = new AtomicInteger();
      var map = new A4TtlMap<String, String>(2, v -> expired.incrementAndGet());
      assertFalse(map.refresh("missing", 100));
      map.put("a", "1", 1);
      Thread.sleep(10);
      assertFalse(map.refresh("a", 100));
      assertEquals(1, expired.get());
      assertEquals(0, map.size());
    });

    it("Sweeps expired entries and notifies exactly once", () -> {
      var expired = new AtomicInteger();
      var map = new A4TtlMap<String, String>(4, v -> expired.incrementAndGet());
      map.put("a", "1", 1);
      map.put("b", "2", -1);
      Thread.sleep(10);
      var removed = map.removeExpired(System.currentTimeMillis());
      assertEquals(1, removed.size());
      assertEquals("1", removed.get(0));
      assertEquals(1, expired.get());
      assertEquals(1, map.size());
      assertNull(map.get("a"));
      assertEquals("2", map.get("b"));
    });

    it("Notifies when an entry is evicted by capacity", () -> {
      var expired = new AtomicInteger();
      var map = new A4TtlMap<String, String>(1, v -> expired.incrementAndGet());
      map.put("a", "1", -1);
      map.put("b", "2", -1);
      assertEquals(1, map.size());
      assertNull(map.get("a"));
      assertEquals("2", map.get("b"));
      assertEquals(1, expired.get());
    });

    it("Removes only the expected key/value pair", () -> {
      var map = new A4TtlMap<String, String>(2, v -> {
      });
      map.put("a", "1", -1);
      assertFalse(map.remove("a", "2"));
      assertTrue(map.remove("a", "1"));
      assertFalse(map.remove("a", "1"));
      assertEquals(0, map.size());
    });

    it("Notifies for every entry when cleared", () -> {
      var expired = new AtomicInteger();
      var map = new A4TtlMap<String, String>(4, v -> expired.incrementAndGet());
      map.put("a", "1", -1);
      map.put("b", "2", -1);
      map.clear();
      assertEquals(0, map.size());
      assertEquals(2, expired.get());
    });

    it("Clears all entries and resets the size", () -> {
      var map = new A4TtlMap<String, String>(4, v -> {
      });
      map.put("a", "1", -1);
      map.put("b", "2", -1);
      map.clear();
      assertEquals(0, map.size());
      assertTrue(map.values().isEmpty());
    });

    it("Removes entries idempotently", () -> {
      var map = new A4TtlMap<String, String>(4, v -> {
      });
      map.put("a", "1", -1);
      assertEquals("1", map.removeAndGet("a"));
      assertNull(map.removeAndGet("a"));
      assertEquals(0, map.size());
    });

    it("Rejects invalid configuration and values", () -> {
      assertThrows(IllegalArgumentException.class, () -> new A4TtlMap<String, String>(0, v -> {
      }));
      var map = new A4TtlMap<String, String>(1, v -> {
      });
      assertThrows(IllegalArgumentException.class, () -> map.put(null, "v", -1));
      assertThrows(IllegalArgumentException.class, () -> map.put("k", null, -1));
      assertThrows(IllegalArgumentException.class, () -> map.get(null));
    });
  }
}
