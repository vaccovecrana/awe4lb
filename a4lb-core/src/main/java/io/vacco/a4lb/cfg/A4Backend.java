package io.vacco.a4lb.cfg;

public class A4Backend {

  public enum State { Up, Down }

  public A4Sock addr;
  public Integer weight, priority;

  public State state = State.Up;
  public A4RxTx rxTx = new A4RxTx();
  public int connections = 0;

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

  public void trackConnOpen() {
    this.connections = this.connections + 1;
  }

  public void trackConnClose() {
    this.connections = this.connections - 1;
    if (this.connections < 0) {
      this.connections = 0; // TODO check that this doesn't happen
    }
  }

  @Override public String toString() {
    return String.format(
        "%s[w: %d, p: %d, s: %s, c: %d]",
        addr, weight, priority, state, connections
    );
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
