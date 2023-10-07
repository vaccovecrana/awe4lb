package io.vacco.a4lb;

import com.google.gson.Gson;
import io.vacco.a4lb.cfg.A4Config;
import io.vacco.a4lb.tcp.*;
import io.vacco.a4lb.util.*;
import org.slf4j.*;
import java.util.*;
import java.util.concurrent.*;

public class A4Lb {

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
          tasks.add(new A4Discover(srv.id, match, srvImpl.bkSel, gson, exSvc));
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

}
