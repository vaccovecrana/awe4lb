package io.vacco.a4lb.cfg;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class A4Pool {

  public enum Type {
    RoundRobin, LeastConn, IpHash
  }

  public Type type;
  public A4Backend[] hosts;

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

}
