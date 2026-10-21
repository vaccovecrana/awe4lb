package io.vacco.a4lb.sel;

import io.vacco.a4lb.cfg.A4RxTx;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Per-backend counters shared by the selection and proxy paths. All mutation is
 * lock-free so TCP/UDP worker threads never contend on a monitor.
 */
public class A4BackendContext {

  public final A4RxTx rxTx = new A4RxTx();

  private final AtomicInteger connections = new AtomicInteger();
  private final AtomicLong totalConnections = new AtomicLong();
  private final LongAdder rxBytes = new LongAdder();
  private final LongAdder txBytes = new LongAdder();

  public void trackConn(boolean isConnect) {
    if (isConnect) {
      this.connections.incrementAndGet();
      this.totalConnections.incrementAndGet();
    } else {
      this.connections.updateAndGet(v -> v > 0 ? v - 1 : 0);
    }
  }

  public void trackRxTx(boolean isRx, long bytes) {
    if (bytes > 0) {
      if (isRx) {
        rxBytes.add(bytes);
        rxTx.updateRx(bytes);
      } else {
        txBytes.add(bytes);
        rxTx.updateTx(bytes);
      }
    }
  }

  public int activeConnections() {
    return connections.get();
  }

  public long totalConnections() {
    return totalConnections.get();
  }

  public long rxBytes() {
    return rxBytes.sum();
  }

  public long txBytes() {
    return txBytes.sum();
  }

}
