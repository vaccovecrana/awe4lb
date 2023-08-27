package io.vacco.a4lb.cfg;

public class A4MatchOp {

  public A4StringOp sni, host;

  public A4MatchOp sni(A4StringOp sni) {
    this.sni = sni;
    return this;
  }

  public A4MatchOp host(A4StringOp host) {
    this.host = host;
    return this;
  }

  @Override public String toString() {
    return
        sni != null ? String.format("sni %s", sni)
        : host != null ? String.format("host %s", host)
        : "A4MatchOp - ?";
  }
}
