package io.vacco.a4lb.cfg;

public class A4Backend {

  public A4Sock addr;
  public Integer weight;
  public Integer priority;

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

}
