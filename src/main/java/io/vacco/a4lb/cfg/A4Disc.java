package io.vacco.a4lb.cfg;

public class A4Disc {

  public A4DiscHttp http;
  public A4DiscExec exec;

  public int intervalMs = 10000, timeoutMs = 9900;

  public A4Disc http(A4DiscHttp http) {
    this.http = http;
    return this;
  }

  public A4Disc exec(A4DiscExec exec) {
    this.exec = exec;
    return this;
  }

  public A4Disc intervalMs(int intervalMs) {
    this.intervalMs = intervalMs;
    return this;
  }

  public A4Disc timeoutMs(int timeoutMs) {
    this.timeoutMs = timeoutMs;
    return this;
  }

}
