package io.vacco.a4lb.service;

import com.google.gson.Gson;
import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.impl.A4Lb;
import io.vacco.a4lb.util.*;
import java.io.Closeable;
import java.io.File;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import static java.lang.String.format;

public class A4Service implements Closeable {

  public static final String New = "new";

  private final ReentrantLock instanceLock = new ReentrantLock();
  private final Gson gson;

  public A4Lb instance;

  public A4Service(Gson gson) {
    this.gson = Objects.requireNonNull(gson);
  }

  private <T> T lockInstanceAnd(Supplier<T> then) {
    instanceLock.lock();
    try {
      return then.get();
    } finally {
      instanceLock.unlock();
    }
  }

  public A4ConfigState setActive(A4Config config) {
    return lockInstanceAnd(() -> {
      var state = new A4ConfigState();
      if (instance != null) {
        A4Io.close(instance);
        state.inactive = instance.config;
        this.instance = null;
      }
      if (config != null) {
        this.instance = new A4Lb(config, gson).open();
        state.active = instance.config;
      }
      return state;
    });
  }

  public Collection<A4Validation> update(File configRoot, String configId, A4Config config) {
    if (config == null) {
      return List.of(A4Validation.ofMessage("Configuration is required"));
    }
    var effectiveId = New.equals(configId) ? config.id : configId;
    if (effectiveId == null || config.id == null || !effectiveId.equals(config.id)) {
      return List.of(A4Validation.ofMessage(format("Config id mismatch: [%s, %s]", effectiveId, config.id)));
    }
    var errList = A4Configs.save(configRoot, gson, config);
    return errList.isEmpty() ? Collections.emptyList() : A4Valid.validationsOf(errList);
  }

  public A4Stats stats() {
    return lockInstanceAnd(() -> instance != null ? instance.stats() : new A4Stats());
  }

  @Override public void close() {
    A4Io.close(instance);
  }

}
