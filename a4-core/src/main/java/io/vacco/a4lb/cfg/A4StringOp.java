package io.vacco.a4lb.cfg;

import static io.vacco.a4lb.util.A4MatchOps.*;

public class A4StringOp {

  /*
   * Come think of it, these seem to be the two most common filtering
   * operations for SNI matches. I'll add more if there's demand for it.
   */
  public String equals, endsWith, startsWith;

  public A4StringOp equals(String equals) {
    this.equals = equals;
    return this;
  }

  public A4StringOp endsWith(String endsWith) {
    this.endsWith = endsWith;
    return this;
  }

  public A4StringOp startsWith(String startsWith) {
    this.startsWith = startsWith;
    return this;
  }

  public String opId() {
    return equals != null ? OpEquals
      : endsWith != null ? OpEndsWith
      : startsWith != null ? OpStartsWith
      : "?";
  }

  public String opVal() {
    return equals != null ? equals
      : endsWith != null ? endsWith
      : startsWith != null ? startsWith
      : "?";
  }

  @Override public String toString() {
    return String.format("%s(%s)", opId(), opVal());
  }

}
