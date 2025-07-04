package io.vacco.a4lb.cfg;

public class A4Match {

  public A4MatchOp      op;
  public A4Pool         pool;
  public A4Disc         discover;
  public A4HealthCheck  healthCheck;
  public A4Tls          tls;

  public A4Match op(A4MatchOp op) {
    this.op = op;
    return this;
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

  public String matchLabel() {
    return op == null ? "any" : op.toString();
  }

  @Override public String toString() {
    return matchLabel();
  }

  @Override public int hashCode() {
    return matchLabel().hashCode();
  }

}
