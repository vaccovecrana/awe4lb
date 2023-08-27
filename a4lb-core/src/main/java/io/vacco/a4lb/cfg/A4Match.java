package io.vacco.a4lb.cfg;

import java.util.Arrays;
import java.util.List;

public class A4Match {

  public A4MatchOp[] and, or;
  public A4Pool pool;

  public A4Match and(A4MatchOp ... and) {
    this.and = and;
    return this;
  }

  public List<A4MatchOp> andOps() {
    return Arrays.asList(and);
  }

  public A4Match or(A4MatchOp ... or) {
    this.or = or;
    return this;
  }

  public List<A4MatchOp> orOps() {
    return Arrays.asList(or);
  }

  public A4Match pool(A4Pool pool) {
    this.pool = pool;
    return this;
  }

}
