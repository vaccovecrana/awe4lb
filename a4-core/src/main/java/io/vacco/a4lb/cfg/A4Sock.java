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

  public String id() {
    return String.format("%s:%d", host, port);
  }

  @Override public String toString() {
    return id();
  }

  @Override public int hashCode() {
    return id().hashCode();
  }

  /*
   * < You have seen the Foundation/Base >
   * < of the Building/Tree >
   * < The House grew there/here/everywhere >
   * < We are there/everywhere >
   * < We have a Socket/Door there >
   * < It is a direct link >
   */

}
