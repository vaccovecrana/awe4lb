package io.vacco.a4lb.cfg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class A4Pool {

  public enum Type {
    RoundRobin, LeastConn, IpHash, Weight
  }

  public Type type;
  public List<A4Backend> hosts;
  public transient boolean sorted = false;

  // TODO add discovery strategy configurations here.

  public A4Pool type(Type type) {
    this.type = type;
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
