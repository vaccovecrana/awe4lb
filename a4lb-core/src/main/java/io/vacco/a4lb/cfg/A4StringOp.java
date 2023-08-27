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

  @Override public String toString() {
    return String.format("%s %s",
        equals != null ? "=="
            : contains != null ? "contains"
            : startsWith != null ? "startsWith"
            : endsWith != null ? "endsWith"
            : "?",
        equals != null ? equals
            : contains != null ? contains
            : startsWith != null ? startsWith
            : endsWith != null ? endsWith
            : "?"
    );
  }
}
