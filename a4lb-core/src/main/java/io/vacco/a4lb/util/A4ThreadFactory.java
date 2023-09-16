package io.vacco.a4lb.util;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;

public class A4ThreadFactory implements ThreadFactory {

  private final String prefix;

  public A4ThreadFactory(String prefix) {
    this.prefix = Objects.requireNonNull(prefix);
  }

  @Override public Thread newThread(Runnable r) {
    var t = new Thread(r);
    t.setName(String.format("%s-%x", prefix, t.hashCode()));
    return t;
  }
}
