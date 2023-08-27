package io.vacco.a4lb.cfg;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class A4Pool {

  public enum Type {
    Weight, RoundRobin, LeastConn, IpHash
  }

  public Type type;
  public A4Backend[] hosts;

  // TODO add discovery strategy configurations here.

  public A4Pool type(Type type) {
    this.type = type;
    return this;
  }

  public A4Pool hosts(A4Backend ... hosts) {
    this.hosts = hosts;
    return this;
  }

  public List<A4Backend> hostList() {
    return hosts == null ? Collections.emptyList() : Arrays.asList(hosts);
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
