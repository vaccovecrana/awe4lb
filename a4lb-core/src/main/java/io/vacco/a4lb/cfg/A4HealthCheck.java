package io.vacco.a4lb.cfg;

public class A4HealthCheck {

  public long intervalMs; // health check probe period in milliseconds.
  public long timeoutMs;  // health check probe timeout in milliseconds.
  public A4Exec exec;

  public A4HealthCheck withIntervalMs(long intervalMs) {
    this.intervalMs = intervalMs;
    return this;
  }

  public A4HealthCheck withTimeoutMs(long timeoutMs) {
    this.timeoutMs = timeoutMs;
    return this;
  }

  public A4HealthCheck withExec(A4Exec exec) {
    this.exec = exec;
    return this;
  }

}
