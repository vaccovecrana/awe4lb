package io.vacco.a4lb.util;

import java.util.Objects;

public class A4Base36 {

  private static final String BASE36_CHARS = "0123456789abcdefghijklmnopqrstuvwxyz";
  private static final int MAX_VALUE_4 = 1679616; // 36^4
  private static final int Length = 4;

  public static String hash4(String in) {
    int hash = Math.abs(Objects.requireNonNull(in).hashCode());
    hash = hash % MAX_VALUE_4; // Reduce to fit within base-36^4 range
    var result = new char[Length];
    for (int i = Length; i > 0; i--) {
      result[i - 1] = BASE36_CHARS.charAt(hash % 36);
      hash /= 36;
    }
    return new String(result);
  }

  public static void main(String[] args) {
    System.out.println(hash4("Hello world!"));
  }

}
