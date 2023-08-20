package io.vacco.a4lb.cfg;

public class A4HealthCheck {

  public long intervalMs; // health check probe period in milliseconds.
  public long timeoutMs;  // health check probe timeout in milliseconds.

  public String execCommand;    // a custom script to invoke with [host, port] as arguments.
  public String execPassOutput; // if the script produces this output, the host is marked healthy.
  public String execFailOutput; // if the script produces this output, the host is marked unhealthy.

}
