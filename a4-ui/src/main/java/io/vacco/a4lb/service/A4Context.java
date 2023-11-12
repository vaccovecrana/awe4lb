package io.vacco.a4lb.service;

import com.google.gson.*;
import io.vacco.a4lb.cfg.A4Format;
import io.vacco.a4lb.util.*;
import io.vacco.a4lb.web.A4Api;
import org.slf4j.Logger;
import java.io.Closeable;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static io.vacco.a4lb.util.A4Configs.*;
import static io.vacco.shax.logging.ShOption.*;
import static java.lang.String.join;

public class A4Context implements Closeable {

  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
  public  final A4Service service = new A4Service(gson);

  private Logger log;
  private A4Api api;

  private void loadLastActive(File configRoot) {
    var activeConfigs = configList(configRoot, gson)
        .filter(cfg -> cfg.active)
        .collect(Collectors.toCollection(ArrayList::new));
    var cfg = activeConfigs.isEmpty() ? null : activeConfigs.remove(0);
    if (cfg != null) {
      if (activeConfigs.size() > 1) {
        log.warn("multiple active configurations found, starting configuration [{}]", cfg.id);
        for (var cfg0 : activeConfigs) {
          syncFs(configRoot, gson, cfg0, false);
        }
      } else {
        log.info("loading last active configuration: [{}]", cfg.id);
      }
      service.setActive(cfg);
    } else {
      log.info("no active configuration found");
    }
  }

  public void init(A4Flags fl) {
    setSysProp(IO_VACCO_SHAX_PRETTYPRINT, "true");
    setSysProp(IO_VACCO_SHAX_DEVMODE, fl.logFormat == A4Format.text ? "true" : "false");
    setSysProp(IO_VACCO_SHAX_LOGLEVEL, fl.logLevel.toString());
    this.log = org.slf4j.LoggerFactory.getLogger(A4Service.class);
    log.info(
        join("\n", "",
            "                       __ __  ____  ",
            "  ____ __      _____  / // / / / /_ ",
            " / __ `/ | /| / / _ \\/ // /_/ / __ \\",
            "/ /_/ /| |/ |/ /  __/__  __/ / /_/ /",
            "\\__,_/ |__/|__/\\___/  /_/ /_/_.___/ "
        )
    );
    try {
      var configRoot = Objects.requireNonNull(fl.root);
      if (configRoot.isDirectory()) {
        this.api = new A4Api(configRoot, service, fl, gson).open();
        loadLastActive(configRoot);
      } else {
        var cfg = loadFromOrFail(configRoot, gson);
        service.setActive(syncFs(configRoot, gson, cfg, true));
      }
    } catch (Exception e) {
      log.error("unable to initialize load balancer context", e);
      throw new IllegalStateException(e);
    }
  }

  @Override public void close() {
    A4Io.close(service);
    A4Io.close(api);
  }

}
