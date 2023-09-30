package io.vacco.a4lb.cfg;

import java.util.*;
import java.util.stream.Collectors;

public class A4Pool {

  public enum Type {
    RoundRobin, LeastConn, IpHash, Weight
  }

  public Type type;
  public List<A4Backend> hosts = new ArrayList<>();
  public A4Disc discover;
  public boolean openTls = false;

  public transient Random rnd = new Random(A4Config.Seed);
  public transient int rrVal = 0;

  public A4Pool type(Type type) {
    this.type = type;
    return this;
  }

  public A4Pool discover(A4Disc discover) {
    this.discover = discover;
    return this;
  }

  public A4Pool hosts(List<A4Backend> hosts) {
    this.hosts = new ArrayList<>(hosts);
    return this;
  }

  public A4Pool hosts(A4Backend ... hosts) {
    return hosts(new ArrayList<>(Arrays.asList(hosts)));
  }

  public List<A4Backend> hostList() {
    return hosts;
  }

  public List<A4Backend> upHosts() {
    return hosts.stream()
        .filter(bk -> bk.state == A4Backend.State.Up)
        .collect(Collectors.toList());
  }

  @Override public int hashCode() {
    if (hosts == null || hosts.isEmpty()) {
      return 0;
    }
     var hlCodes = hosts.stream()
        .map(h -> String.format("%x", h.hashCode()))
        .collect(Collectors.joining(":"));
    return hlCodes.hashCode();
  }

  /*
   * < We are at War/Hostile Takeover >
   * < The Hiss is the Opposing Force/Foreign Power >
   * < We will provide >
   * < Countermeasures/Strategy >
   * < Accept/ingest them >
   * < at Control Points >
   * < These Countermeasures/Missions >
   * < will benefit you >
   * < We will give Resources/Rewards >
   * < for each completed Countermeasure >
   */

}
