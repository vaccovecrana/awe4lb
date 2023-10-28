package io.vacco.a4lb;

import am.ik.yavi.core.ConstraintViolations;
import com.google.gson.Gson;
import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.impl.A4Lb;
import io.vacco.a4lb.util.*;
import io.vacco.a4lb.web.A4Api;
import org.slf4j.Logger;
import java.io.Closeable;
import java.io.*;
import java.util.*;
import java.util.stream.Stream;

import static io.vacco.shax.logging.ShOption.*;
import static java.lang.String.*;

public class A4Service implements Closeable {

  public static final String ExtJson = ".json";

  private Logger  log;
  public  A4Api   api;
  public  Gson    gson = new Gson();
  public  A4Lb    instance;
  public  File    configRoot;

  private File cfgFileOf(String id) {
    return new File(configRoot, format("%s%s", id, ExtJson));
  }

  private A4Config syncFs(A4Config cfg, boolean active) {
    var cfgFile = cfgFileOf(cfg.id);
    cfg.active = active;
    try (var fw = new FileWriter(cfgFile)) {
      gson.toJson(cfg, fw);
      return cfg;
    } catch (Exception e) {
      cfg.active = false;
      throw new IllegalStateException("Unable to write configuration: " + cfgFile.getAbsolutePath(), e);
    }
  }

  private void setActive(File cfgFile) {
    try {
      if (this.instance != null) {
        A4Io.close(instance);
        syncFs(instance.config, false);
      }
      var cfg = A4Configs.loadFrom(cfgFile.toURI().toURL(), gson);
      this.instance = new A4Lb(syncFs(cfg, true), gson).open();
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.debug("{} - unable to initialize load balancer instance", cfgFile.getAbsolutePath(), e);
      } else {
        log.error("{} - unable to initialize load balancer instance - {}", cfgFile.getAbsolutePath(), e.getMessage());
      }
    }
  }

  public void setActive(String cfgId) {
    setActive(cfgFileOf(cfgId));
  }

  public ConstraintViolations add(A4Config cfg) {
    var errors = A4Valid.validate(cfg);
    if (errors.isEmpty()) {
      syncFs(cfg, false);
    }
    return errors;
  }

  public Stream<A4Config> rootConfigs() {
    var files = configRoot.listFiles();
    if (files != null) {
      return Arrays.stream(files)
          .filter(f -> f.getName().endsWith(ExtJson))
          .map(f -> A4Configs.loadFrom(f, gson));
    }
    return Stream.empty();
  }

  public A4Service init(A4Flags fl) {
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
      this.configRoot = Objects.requireNonNull(fl.root);
      if (configRoot.isDirectory()) {
        this.api = new A4Api(this, fl, gson).open();
        rootConfigs()
            .filter(cfg -> cfg.active)
            .findFirst().ifPresent(cfg -> {
              log.info("loading last active configuration: [{}]", cfg.id);
              setActive(cfg.id);
            });
      } else {
        setActive(configRoot);
      }
    } catch (Exception e) {
      log.error("unable to initialize load balancer service", e);
      throw new IllegalStateException(e);
    }
    return this;
  }

  @Override public void close() {
    A4Io.close(api);
    A4Io.close(instance);
  }

}
