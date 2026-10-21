package io.vacco.a4lb;

import com.google.gson.Gson;
import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.util.A4Configs;
import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;

import java.nio.file.Files;

import static j8spec.J8Spec.*;
import static org.junit.Assert.*;

@DefinedOrder
@RunWith(J8SpecRunner.class)
public class A4ConfigsTest {

  private static A4Config validConfig(String id) {
    return new A4Config()
      .id(id)
      .description("test configuration")
      .server(new A4Server()
        .id("http")
        .addr(new A4Sock().host("127.0.0.1").port(18080))
        .match(new A4Match().pool(new A4Pool().hosts(
          new A4Backend().addr(new A4Sock().host("127.0.0.1").port(9000))
        )))
      );
  }

  static {
    it("Inflates defaults for TCP matches", () -> {
      var cfg = new A4Config().id("cfg").description("desc")
        .server(new A4Server().id("srv").addr(new A4Sock().host("0.0.0.0").port(8080))
          .match(new A4Match()));
      A4Configs.inflate(cfg);
      var match = cfg.servers.get(0).match.get(0);
      assertNotNull(match.pool);
      assertNotNull(match.pool.hosts);
      assertTrue(match.pool.hosts.isEmpty());
      assertNotNull(match.healthCheck);
      assertEquals(A4HealthCheck.DefaultIntervalMs, match.healthCheck.intervalMs);
      assertEquals(A4HealthCheck.DefaultTimeoutMs, match.healthCheck.timeoutMs);
    });

    it("Deflates a discovered match without a static pool", () -> {
      var discover = new A4Disc();
      discover.intervalMs = A4Disc.DefaultIntervalMs;
      discover.timeoutMs = A4Disc.DefaultTimeoutMs;
      var cfg = new A4Config().id("cfg").description("desc")
        .server(new A4Server().id("srv").addr(new A4Sock().host("0.0.0.0").port(8080))
          .match(new A4Match()
            .discover(discover)
            .pool(new A4Pool().hosts(new A4Backend().addr(new A4Sock().host("127.0.0.1").port(9000))))));
      var out = A4Configs.deflate(cfg, new Gson());
      var match = out.servers.get(0).match.get(0);
      assertNull(match.pool);
      assertNull(match.discover.intervalMs);
      assertNull(match.discover.timeoutMs);
    });

    it("Saves, lists, loads and deletes configurations", () -> {
      var root = Files.createTempDirectory("a4-configs");
      var gson = new Gson();
      var cfg = validConfig("cfg-01");
      var errors = A4Configs.save(root.toFile(), gson, cfg);
      assertTrue(errors.isEmpty());
      assertTrue(A4Configs.configFileOf(root.toFile(), "cfg-01").exists());
      assertEquals(1, A4Configs.configList(root.toFile(), gson).count());

      var loaded = A4Configs.loadFromOrFail(A4Configs.configFileOf(root.toFile(), "cfg-01"), gson);
      assertEquals("cfg-01", loaded.id);
      assertFalse(loaded.servers.get(0).match.get(0).pool.hosts.isEmpty());

      assertTrue(A4Configs.delete(root.toFile(), "cfg-01"));
      assertFalse(A4Configs.delete(root.toFile(), "cfg-01"));
    });

    it("Creates missing configuration directories", () -> {
      var root = Files.createTempDirectory("a4-configs").resolve("nested");
      var gson = new Gson();
      A4Configs.save(root.toFile(), gson, validConfig("cfg-02"));
      assertTrue(A4Configs.configFileOf(root.toFile(), "cfg-02").exists());
    });

    it("Returns validation errors instead of persisting an invalid configuration", () -> {
      var root = Files.createTempDirectory("a4-configs");
      var errors = A4Configs.save(root.toFile(), new Gson(), new A4Config());
      assertFalse(errors.isEmpty());
      assertEquals(0, A4Configs.configList(root.toFile(), new Gson()).count());
    });

    it("Rejects a null configuration", () -> {
      var root = Files.createTempDirectory("a4-configs");
      assertThrows(IllegalArgumentException.class, () -> A4Configs.inflate(null));
      assertThrows(IllegalArgumentException.class, () -> A4Configs.save(root.toFile(), new Gson(), null));
    });
  }
}
