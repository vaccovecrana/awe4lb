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

  private final Gson gson;
  private final A4Config config;
  private final ExecutorService exSvc = Executors.newCachedThreadPool(new A4ThreadFactory("awe4lb"));

  public A4Lb(A4Config config, Gson gson) {
    this.gson = Objects.requireNonNull(gson);
    this.config = Objects.requireNonNull(config);
    var configErrors = A4Valid.A4ConfigVld.validate(config);
    if (!configErrors.isEmpty()) {
      throw new A4Exceptions.A4ConfigException(configErrors);
    }
  }

  public void start() throws InterruptedException {
    log.info("Starting");
    var tasks = new ArrayList<Callable<Void>>();
    for (var srv : config.servers) {
      var srvImpl = new A4TcpSrv(A4Io.newSelector(), srv, exSvc); // TODO this will need to accommodate UDP servers too.
      tasks.add(srvImpl);
      for (var match : srv.match) {
        tasks.add(new A4TcpHealth(exSvc, srv.id, match, srvImpl.bkSel));
        if (match.discover != null) {
          tasks.add(new A4Discover(match, srvImpl.bkSel, gson, exSvc));
        }
      }
    }
    log.info("Started");
    exSvc.invokeAll(tasks);
  }

  public void stop() {
    log.info("Stopping");
    exSvc.shutdownNow();
    log.info("Stopped");
  }

  public static void main(String[] args) { // TODO this needs to move to a higher level class (for API/UI functionality)
    A4Lb a4lb = null;
    try {
      if (args == null || args.length != 1) {
        throw new IllegalArgumentException("Must provide a single configuration file argument.");
      }
      var cfgFile = new File(args[0]);
      if (!cfgFile.exists()) {
        throw new IllegalArgumentException("Missing configuration file " + cfgFile.getAbsolutePath());
      }
      log.info(
          String.join("\n", "",
              "                       __ __  ____  ",
              "  ____ __      _____  / // / / / /_ ",
              " / __ `/ | /| / / _ \\/ // /_/ / __ \\",
              "/ /_/ /| |/ |/ /  __/__  __/ / /_/ /",
              "\\__,_/ |__/|__/\\___/  /_/ /_/_.___/ "
          )
      );
      var g = new Gson();
      var cfg = A4Configs.loadFrom(cfgFile.toURI().toURL(), g);
      a4lb = new A4Lb(cfg, g);
      a4lb.start();
    } catch (Exception e) {
      log.error("Unable to initialize load balancer", e);
      if (a4lb != null) {
        a4lb.stop();
      }
    }
  }

}
