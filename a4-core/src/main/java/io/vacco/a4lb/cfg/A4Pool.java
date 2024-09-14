package io.vacco.a4lb.cfg;

import java.util.*;
import java.util.stream.Collectors;

public class A4Pool {

  public A4PoolType type;
  public List<A4Backend> hosts;
  public Boolean openTls;

  public A4Pool hosts(List<A4Backend> hosts) {
    this.hosts = new ArrayList<>(hosts);
    return this;
  }

  public A4Pool hosts(A4Backend ... hosts) {
    return hosts(Arrays.asList(hosts));
  }

  public A4Pool clear() {
    this.hosts = null;
    return this;
  }

  public List<A4Backend> hostList() {
    return hosts;
  }

  public List<A4Backend> upHosts() {
    return hosts.stream()
        .filter(bk -> bk.state == A4BackendState.Up)
        .collect(Collectors.toList());
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
