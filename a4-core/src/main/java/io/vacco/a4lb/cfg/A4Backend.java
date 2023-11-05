package io.vacco.a4lb.cfg;

public class A4Backend {

  public enum State { Up, Down, Unknown }

  public A4Sock addr;
  public Integer weight, priority;
  public transient State state = State.Unknown;

  public A4Backend addr(A4Sock addr) {
    this.addr = addr;
    return this;
  }

  public A4Backend weight(Integer weight) {
    this.weight = weight;
    return this;
  }

  public A4Backend priority(Integer priority) {
    this.priority = priority;
    return this;
  }

  public A4Backend state(State state) {
    this.state = state;
    return this;
  }

  @Override public String toString() {
    return String.format(
        "%s[w: %d, p: %d, s: %s]",
        addr, weight, priority, state
    );
  }

  @Override public boolean equals(Object obj) {
    if (obj instanceof A4Backend) {
      return obj.hashCode() == this.hashCode();
    }
    return false;
  }

  @Override public int hashCode() {
    return addr != null ? addr.hashCode() : 0;
  }

  /*
   * < Objects of Power are Holders/665 >
   * < to the Other/Blessing >
   * < We hold the reins/law >
   * < We are failing in translating hyperreal concepts >
   * < Secrets/Instructions will be lost >
   * < Ignore this message >
   */

}
