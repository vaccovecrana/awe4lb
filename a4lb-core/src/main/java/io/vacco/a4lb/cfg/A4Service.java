package io.vacco.a4lb.cfg;

public class A4Service {

  public String host;
  public int port;

  public A4HealthCheck healthCheck;

  public A4Service host(String host) {
    this.host = host;
    return this;
  }

  public A4Service port(int port) {
    this.port = port;
    return this;
  }

  public A4Service healthCheck(A4HealthCheck healthCheck) {
    this.healthCheck = healthCheck;
    return this;
  }

}
