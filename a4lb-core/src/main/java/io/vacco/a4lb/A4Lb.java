package io.vacco.a4lb;

import io.vacco.a4lb.cfg.A4Config;
import io.vacco.a4lb.util.A4Exceptions;
import io.vacco.a4lb.util.A4Valid;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class A4Lb {

  private final A4Config config;
  private final ExecutorService lbx = Executors.newCachedThreadPool(r -> {
    var t = new Thread(r);

    /*
     * TODO set thread names:
     *   awe4lb-<server-name>-io
     *   awe4lb-<server-name>-discovery
     *   awe4lb-<server-name>-health
     */

    t.setName("awe4lb-task-pool");
    return t;
  });

  public A4Lb(A4Config config) {
    this.config = Objects.requireNonNull(config);
    var configErrors = A4Valid.A4ConfigVld.validate(config);
    if (!configErrors.isEmpty()) {
      throw new A4Exceptions.A4ConfigException(configErrors);
    }
  }

  public void start() {
    // TODO
    //   load each server definition, and map to A4TcpSrv
    //   next, spin a new thread per server.
    //   Then, spin a second per-server discovery thread (in case the host list is not static). Provides new backend entries.
    //   Then, spin a third per-server health check thread. Marks backend statuses as UP, DOWN, UNKNOWN.
    //   Then, launch a metrics thread. An actor which receives events on a queue from server threads (host/up/down, session rx/tx byte counts, etc.).
  }

}
