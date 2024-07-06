package io.vacco.a4lb.cfg;

import java.util.Objects;

public class A4Validation {

  public String[] args;
  public String format, key, name, message;

  public static A4Validation ofMessage(String message) {
    var v = new A4Validation();
    v.message = Objects.requireNonNull(message);
    return v;
  }

  @Override public String toString() {
    return message != null ? message : "validation[?]";
  }

}
