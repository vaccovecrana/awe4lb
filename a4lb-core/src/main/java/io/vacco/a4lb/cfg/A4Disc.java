package io.vacco.a4lb.cfg;

public class A4Disc {

  public A4DiscHttp http;
  public A4DiscExec exec;

  public A4Disc http(A4DiscHttp http) {
    this.http = http;
    return this;
  }

  public A4Disc exec(A4DiscExec exec) {
    this.exec = exec;
    return this;
  }

}
