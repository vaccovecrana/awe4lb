package io.vacco.a4lb;

import io.vacco.a4lb.cfg.A4Stats;
import io.vacco.a4lb.cfg.A4StatsBackend;
import io.vacco.a4lb.impl.A4StatsRecorder;
import io.vacco.a4lb.sel.A4BackendContext;
import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicLong;

import static j8spec.J8Spec.it;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

@DefinedOrder
@RunWith(J8SpecRunner.class)
public class A4StatsTest {
  static {
    it("Tracks active, total and cumulative backend counters", () -> {
      var ctx = new A4BackendContext();
      assertEquals(0, ctx.activeConnections());
      ctx.trackConn(true);
      ctx.trackConn(true);
      assertEquals(2, ctx.activeConnections());
      assertEquals(2, ctx.totalConnections());
      ctx.trackConn(false);
      assertEquals(1, ctx.activeConnections());
      ctx.trackConn(false);
      ctx.trackConn(false);
      assertEquals(0, ctx.activeConnections());
      assertEquals(2, ctx.totalConnections());

      ctx.trackRxTx(true, 100);
      ctx.trackRxTx(true, 20);
      ctx.trackRxTx(false, 70);
      ctx.trackRxTx(false, -5);
      ctx.trackRxTx(true, 0);
      assertEquals(120, ctx.rxBytes());
      assertEquals(70, ctx.txBytes());
    });

    it("Records rates and a bounded history ring", () -> {
      var now = new AtomicLong(1_000);
      var bytes = new AtomicLong(0);
      var recorder = new A4StatsRecorder(() -> {
        var stats = new A4Stats();
        stats.configId = "cfg";
        stats.timestampMs = now.get();
        var backend = new A4StatsBackend();
        backend.id = "srv|0|127.0.0.1:9000";
        backend.serverId = "srv";
        backend.rxBytes = bytes.get();
        backend.txBytes = bytes.get() * 2;
        stats.backends.add(backend);
        return stats;
      }, 3);

      recorder.tick();
      now.set(2_000);
      bytes.set(1_000);
      recorder.tick();
      now.set(3_000);
      bytes.set(2_500);
      var current = recorder.tick();

      assertEquals(1_500.0, current.backends.get(0).rxBps, 0.001);
      assertEquals(3_000.0, current.backends.get(0).txBps, 0.001);
      assertEquals(3, current.history.timestamps.size());
      assertEquals(1, current.history.series.size());
      assertEquals(3, current.history.series.get(0).rxBps.length);

      now.set(4_000);
      bytes.set(4_500);
      recorder.tick();
      assertEquals(3, recorder.size());
      assertEquals(3, recorder.current().history.timestamps.size());
    });

    it("Rejects invalid recorder capacity", () -> {
      assertThrows(IllegalArgumentException.class, () -> new A4StatsRecorder(A4Stats::new, 0));
    });
  }
}
