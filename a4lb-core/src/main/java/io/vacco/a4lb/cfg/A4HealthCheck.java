package io.vacco.a4lb.cfg;

public class A4HealthCheck {

  public long intervalMs;
  public long timeoutMs;
  public A4Exec exec;

  public A4HealthCheck intervalMs(long intervalMs) {
    this.intervalMs = intervalMs;
    return this;
  }

  public A4HealthCheck timeoutMs(long timeoutMs) {
    this.timeoutMs = timeoutMs;
    return this;
  }

  public A4HealthCheck exec(A4Exec exec) {
    this.exec = exec;
    return this;
  }

}
