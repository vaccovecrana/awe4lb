package io.vacco.a4lb.cfg;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class A4Match {

  public A4MatchOp[] and, or;
  public A4Pool pool;
  public A4Disc discover;
  public A4HealthCheck healthCheck = new A4HealthCheck();

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

  public A4Match discover(A4Disc discover) {
    this.discover = discover;
    return this;
  }

  public A4Match healthCheck(A4HealthCheck healthCheck) {
    this.healthCheck = healthCheck;
    return this;
  }

  public String toStringOp(String opLabel, A4MatchOp[] ops) {
    return Arrays.stream(ops)
        .map(A4MatchOp::toString)
        .collect(Collectors.joining(String.format(" %s ", opLabel)));
  }

  @Override public String toString() {
    return toStringOp(
        and != null ? "and" : "or",
        and != null ? and : or
    );
  }

}
