package io.vacco.a4lb.cfg;

public class A4Disc {

  public static final Integer
      DefaultIntervalMs = 10000,
      DefaultTimeoutMs = 9900;

  public A4DiscHttp http;
  public A4DiscExec exec;
  public A4DiscK8s  k8s;

  public Integer intervalMs, timeoutMs;

}
