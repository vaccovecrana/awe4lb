package io.vacco.a4lb.cfg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class A4Server {

  public String id;
  public A4Sock addr;
  public List<A4Match> match;

  public A4Tls tls;
  public A4Udp udp;

  public A4Server id(String id) {
    this.id = id;
    return this;
  }

  public A4Server addr(A4Sock addr) {
    this.addr = addr;
    return this;
  }

  public A4Server tls(A4Tls tls) {
    this.tls = tls;
    return this;
  }

  public A4Server match(A4Match ... match) {
    this.match = new ArrayList<>(Arrays.asList(match));
    return this;
  }

  /*
   * < We reach/tether >
   * < through Control Points >
   * < into the Bureau/House >
   * < Nexus/Threshold/Area Codes >
   * < allows for access >
   * < You can >
   * < Translocate/Fast-Travel/Construct >
   * < through the Control Points >
   */

}
