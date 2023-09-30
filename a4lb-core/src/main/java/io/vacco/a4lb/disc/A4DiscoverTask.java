package io.vacco.a4lb.disc;

import io.vacco.a4lb.cfg.A4Disc;
import java.util.Objects;
import java.util.concurrent.Callable;

public class A4DiscoverTask implements Callable<Void> {

  private final A4Disc disc;

  public A4DiscoverTask(A4Disc disc) {
    this.disc = Objects.requireNonNull(disc);
  }

  @Override public Void call() throws Exception {
    return null;
  }

}
