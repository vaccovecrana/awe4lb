package io.vacco.a4lb.cfg;

public class A4Service {

  public String host;
  public int port;

  public A4HealthCheck healthCheck;

  public A4Service withHost(String host) {
    this.host = host;
    return this;
  }

  public A4Service withPort(int port) {
    this.port = port;
    return this;
  }

  public A4Service withHealthCheck(A4HealthCheck healthCheck) {
    this.healthCheck = healthCheck;
    return this;
  }

}
