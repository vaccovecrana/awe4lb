package io.vacco.a4lb;

import com.google.gson.Gson;
import io.vacco.a4lb.cfg.A4Config;
import io.vacco.a4lb.tcp.*;
import io.vacco.a4lb.util.*;
import org.slf4j.*;
import org.slf4j.bridge.SLF4JBridgeHandler;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;

public class A4Lb {

  static {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }

  private static final Logger log = LoggerFactory.getLogger(A4Lb.class);

  private final A4Config config;

  private final ExecutorService
      tskEx = Executors.newCachedThreadPool(new A4ThreadFactory("awe4lb-io")),
      dscEx = Executors.newCachedThreadPool(new A4ThreadFactory("awe4lb-discover")),
      hltEx = Executors.newCachedThreadPool(new A4ThreadFactory("awe4lb-health-check"));

  public A4Lb(A4Config config) {
    this.config = Objects.requireNonNull(config);
    var configErrors = A4Valid.A4ConfigVld.validate(config);
    if (!configErrors.isEmpty()) {
      throw new A4Exceptions.A4ConfigException(configErrors);
    }
  }

  public void start() throws InterruptedException {
    log.info("Starting");
    var tasks = new ArrayList<Callable<A4TcpSrv>>();
    for (var srvE : config.servers.entrySet()) {
      tasks.add(() -> new A4TcpSrv(A4Io.newSelector(), srvE.getKey(), srvE.getValue()).updateLoop());
      // TODO spin a second per-server discovery thread (in case the host list is not static). Provides new backend entries.
      // TODO spin a third per-server health check thread. Marks backend statuses as UP, DOWN, UNKNOWN.
    }
    log.info("Started");
    tskEx.invokeAll(tasks);
  }

  public void stop() {
    log.info("Stopping");
    tskEx.shutdownNow();
    dscEx.shutdownNow();
    hltEx.shutdownNow();
    log.info("Stopped");
  }

  public static void main(String[] args) {
    A4Lb a4lb = null;
    try {
      if (args == null || args.length != 1) {
        throw new IllegalArgumentException("Must provide a single configuration file argument.");
      }
      var cfgFile = new File(args[0]);
      if (!cfgFile.exists()) {
        throw new IllegalArgumentException("Missing configuration file " + cfgFile.getAbsolutePath());
      }
      var g = new Gson();
      var cfg = A4Configs.loadFrom(cfgFile.toURI().toURL(), g);
      a4lb = new A4Lb(cfg);
      a4lb.start();
    } catch (Exception e) {
      log.error("Unable to initialize load balancer", e);
      if (a4lb != null) {
        a4lb.stop();
      }
    }
  }

}
