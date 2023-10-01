package io.vacco.a4lb.cfg;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class A4Server {

  public String id;
  public A4Sock addr;
  public A4Tls tls;
  public A4Match[] match;

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
    this.match = match;
    return this;
  }

  public List<A4Match> matchList() {
    return match == null ? Collections.emptyList() : Arrays.asList(match);
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
