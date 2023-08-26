package io.vacco.a4lb.cfg;

public class A4Service {

  public A4Sock addr;
  public A4HealthCheck healthCheck;

  public A4Service addr(A4Sock addr) {
    this.addr = addr;
    return this;
  }

  public A4Service healthCheck(A4HealthCheck healthCheck) {
    this.healthCheck = healthCheck;
    return this;
  }

}
