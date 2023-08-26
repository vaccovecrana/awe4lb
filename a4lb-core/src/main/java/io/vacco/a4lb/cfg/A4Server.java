package io.vacco.a4lb.cfg;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class A4Server {

  public A4Sock addr;
  public A4Tls tls;
  public A4Match[] match;
  public A4HealthCheck healthCheck;

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

  public A4Server healthCheck(A4HealthCheck healthCheck) {
    this.healthCheck = healthCheck;
    return this;
  }

}
