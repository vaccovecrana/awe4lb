package io.vacco.a4lb.cfg;

public class A4Service {

  public A4Sock addr;
  public A4Tls tls;
  public A4Match match;
  public A4HealthCheck healthCheck;

  public A4Service addr(A4Sock addr) {
    this.addr = addr;
    return this;
  }

  public A4Service tls(A4Tls tls) {
    this.tls = tls;
    return this;
  }

  public A4Service match(A4Match match) {
    this.match = match;
    return this;
  }

  public A4Service healthCheck(A4HealthCheck healthCheck) {
    this.healthCheck = healthCheck;
    return this;
  }

}
