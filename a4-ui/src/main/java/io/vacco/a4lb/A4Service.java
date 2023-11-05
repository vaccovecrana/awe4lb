package io.vacco.a4lb;

import am.ik.yavi.core.ConstraintViolations;
import com.google.gson.*;
import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.impl.A4Lb;
import io.vacco.a4lb.util.*;
import io.vacco.a4lb.web.A4Api;
import org.slf4j.Logger;
import java.io.Closeable;
import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import static io.vacco.shax.logging.ShOption.*;
import static java.lang.String.*;

public class A4Service implements Closeable {

  public static final String ExtJson = ".json";

  private Logger  log;
  private final ReentrantLock instanceLock = new ReentrantLock();

  public  A4Api   api;
  public  Gson    gson = new GsonBuilder().setPrettyPrinting().create();
  public  A4Lb    instance;
  public  File    configRoot;

  private File cfgFileOf(String id) {
    return new File(configRoot, format("%s%s", id, ExtJson));
  }

  private A4Config syncFs(A4Config config, boolean markActive) {
    var cfgFile = cfgFileOf(config.id);
    config.active = markActive;
    try (var fw = new FileWriter(cfgFile)) {
      gson.toJson(config, fw);
      return config;
    } catch (Exception e) {
      config.active = false;
      throw new IllegalStateException("Unable to write configuration: " + cfgFile.getAbsolutePath(), e);
    }
  }

  private void lockInstanceAnd(Runnable then) {
    instanceLock.lock();
    try {
      then.run();
    } finally {
      instanceLock.unlock();
    }
  }

  private void setActive(File configFile) {
    lockInstanceAnd(() -> {
      A4Io.close(instance);
      if (this.instance != null) {
        syncFs(instance.config, false);
      } try {
        var cfg = A4Configs.loadFromOrFail(configFile.toURI().toURL(), gson);
        this.instance = new A4Lb(syncFs(cfg, true), gson).open();
      } catch (Exception e) {
        if (log.isDebugEnabled()) {
          log.debug("{} - unable to initialize load balancer instance", configFile.getAbsolutePath(), e);
        } else {
          log.error("{} - unable to initialize load balancer instance - {}", configFile.getAbsolutePath(), e.getMessage());
        }
      }
    });
  }

  public void setActive(String configId) {
    setActive(cfgFileOf(configId));
  }

  public ConstraintViolations add(A4Config config) {
    var errors = A4Valid.validate(config);
    if (errors.isEmpty()) {
      syncFs(config, false);
    }
    return errors;
  }

  public boolean delete(String configId) {
    var cfgFile = cfgFileOf(configId);
    if (cfgFile.exists() && cfgFile.isFile()) {
      lockInstanceAnd(() -> {
        if (instance != null && instance.config.id.equals(configId)) {
          A4Io.close(instance);
        }
      });
      instanceLock.lock();
      return cfgFile.delete();
    } else {
      log.warn("will not delete invalid configuration file: {}", cfgFile.getAbsolutePath());
      return false;
    }
  }

  public Stream<A4Config> rootConfigs() {
    var files = configRoot.listFiles();
    if (files != null) {
      return Arrays.stream(files)
          .filter(f -> f.getName().endsWith(ExtJson))
          .map(f -> {
            try {
              return A4Configs.loadFromOrFail(f.toURI().toURL(), gson);
            } catch (Exception e) {
              if (log.isDebugEnabled()) {
                log.debug("unable to load configuration from file " + f.getAbsolutePath(), e);
              } else {
                log.warn("unable to load configuration from file {} - {}", f.getAbsolutePath(), A4Exceptions.messageFor(e));
              }
              return null;
            }
          })
          .filter(Objects::nonNull);
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
