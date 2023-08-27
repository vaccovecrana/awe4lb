package io.vacco.a4lb.cfg;

public class A4Probe {

  public boolean enabled;
  public A4Sock bind;

  public A4Probe enabled(boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  public A4Probe bind(A4Sock bind) {
    this.bind = bind;
    return this;
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
