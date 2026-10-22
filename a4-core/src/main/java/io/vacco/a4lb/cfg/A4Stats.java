package io.vacco.a4lb.cfg;

import java.util.ArrayList;
import java.util.List;

/**
 * Snapshot returned by the statistics endpoint. {@code backends} is the current
 * state; {@code history} is the server-side ring buffer rendered by the UI.
 */
public class A4Stats {

  public long timestampMs = System.currentTimeMillis();
  public String configId;
  public List<A4StatsServer> servers = new ArrayList<>();
  public List<A4StatsBackend> backends = new ArrayList<>();
  public A4StatsHistory history = new A4StatsHistory();

}
