package io.vacco.a4lb.impl;

import io.vacco.a4lb.cfg.*;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Samples the live backend snapshot on a fixed cadence and keeps a bounded
 * server-side ring buffer. Rates are derived from cumulative counters so the UI
 * can render history without knowing the polling interval.
 */
public class A4StatsRecorder {

  public static final int DefaultCapacity = 300;
  public static final long DefaultIntervalMs = 1000;

  private final Supplier<A4Stats> snapshot;
  private final int capacity;
  private final ArrayDeque<A4Stats> frames = new ArrayDeque<>();
  private A4Stats last;

  public A4StatsRecorder(Supplier<A4Stats> snapshot) {
    this(snapshot, DefaultCapacity);
  }

  public A4StatsRecorder(Supplier<A4Stats> snapshot, int capacity) {
    if (capacity <= 0) {
      throw new IllegalArgumentException("Capacity must be greater than 0");
    }
    this.snapshot = snapshot;
    this.capacity = capacity;
  }

  public synchronized A4Stats tick() {
    var current = snapshot.get();
    if (last != null) {
      var dtMs = current.timestampMs - last.timestampMs;
      if (dtMs > 0) {
        var previous = new LinkedHashMap<String, A4StatsBackend>();
        last.backends.forEach(b -> previous.put(b.id, b));
        for (var b : current.backends) {
          var p = previous.get(b.id);
          if (p != null) {
            b.rxBps = rateOf(p.rxBytes, b.rxBytes, dtMs);
            b.txBps = rateOf(p.txBytes, b.txBytes, dtMs);
          }
        }
      }
    }
    frames.addLast(current);
    while (frames.size() > capacity) {
      frames.removeFirst();
    }
    last = current;
    return current();
  }

  private static double rateOf(long previous, long current, long dtMs) {
    var delta = current - previous;
    return delta > 0 ? delta * 1000.0 / dtMs : 0;
  }

  public synchronized A4Stats current() {
    if (last == null) {
      var out = snapshot.get();
      out.history = new A4StatsHistory();
      return out;
    }
    var out = new A4Stats();
    out.timestampMs = last.timestampMs;
    out.configId = last.configId;
    out.backends = new ArrayList<>(last.backends);
    out.history = historyOf();
    return out;
  }

  private A4StatsHistory historyOf() {
    var history = new A4StatsHistory();
    var n = frames.size();
    var series = new LinkedHashMap<String, A4StatsSeries>();
    for (var frame : frames) {
      history.timestamps.add(frame.timestampMs);
      for (var b : frame.backends) {
        series.computeIfAbsent(b.id, id -> {
          var s = new A4StatsSeries();
          s.id = id;
          s.rxBps = new double[n];
          s.txBps = new double[n];
          return s;
        });
      }
    }
    var idx = 0;
    for (var frame : frames) {
      var byId = new LinkedHashMap<String, A4StatsBackend>();
      frame.backends.forEach(b -> byId.put(b.id, b));
      for (var s : series.values()) {
        var b = byId.get(s.id);
        if (b != null) {
          s.rxBps[idx] = b.rxBps;
          s.txBps[idx] = b.txBps;
        }
      }
      idx++;
    }
    history.series = new ArrayList<>(series.values());
    return history;
  }

  public synchronized int size() {
    return frames.size();
  }

  public synchronized void clear() {
    frames.clear();
    last = null;
  }

}
