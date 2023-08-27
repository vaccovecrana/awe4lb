package io.vacco.a4lb;

import io.vacco.a4lb.cfg.A4Config;

import java.util.Objects;

public class A4Lb {

  private final A4Config config;

  public A4Lb(A4Config config) {
    this.config = Objects.requireNonNull(config);
  }

  public void start() {
    config.servers;
  }

}
