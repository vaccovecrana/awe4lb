package io.vacco.a4lb.cfg;

public class A4Sock {

  public String host;
  public int port;

  public A4Sock host(String host) {
    this.host = host;
    return this;
  }

  public A4Sock port(int port) {
    this.port = port;
    return this;
  }

}
