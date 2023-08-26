package io.vacco.a4lb.cfg;

public class A4StringOp {

  public String equals, contains, startsWith, endsWith;

  public A4StringOp equals(String equals) {
    this.equals = equals;
    return this;
  }

  public A4StringOp contains(String contains) {
    this.contains = contains;
    return this;
  }

  public A4StringOp startsWith(String startsWith) {
    this.startsWith = startsWith;
    return this;
  }

  public A4StringOp endsWith(String endsWith) {
    this.endsWith = endsWith;
    return this;
  }

}
