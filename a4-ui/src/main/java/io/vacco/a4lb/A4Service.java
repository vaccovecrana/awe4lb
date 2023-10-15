package io.vacco.a4lb;

import am.ik.yavi.core.ConstraintViolations;
import com.google.gson.Gson;
import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.impl.A4Lb;
import io.vacco.a4lb.util.*;
import io.vacco.a4lb.web.A4Api;
import org.slf4j.Logger;
import java.io.Closeable;
import java.util.*;

import static io.vacco.shax.logging.ShOption.*;

public class A4Service implements Closeable {

  private Logger  log;
  public  A4Api   api;
  public  Gson    gson = new Gson();
  public  A4Lb    instance;
  public  Map<String, A4Config> configIdx = new TreeMap<>();

  public void setActive(String configId) {
    try {
      if (this.instance != null) {
        A4Io.close(instance);
        instance.config.active = false;
      }
      var cfg = configIdx.get(configId);
      this.instance = new A4Lb(cfg, gson).open();
      cfg.active = true;
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.debug("{} - unable to initialize load balancer instance", configId, e);
      } else {
        log.error("{} - unable to initialize load balancer instance - {}", configId, e.getMessage());
      }
      A4Io.close(instance);
    }
  }

  public A4Service init(A4Flags fl) {
    setSysProp(IO_VACCO_SHAX_PRETTYPRINT, "true");
    setSysProp(IO_VACCO_SHAX_DEVMODE, fl.logFormat == A4Format.text ? "true" : "false");
    setSysProp(IO_VACCO_SHAX_LOGLEVEL, fl.logLevel.toString());
    this.log = org.slf4j.LoggerFactory.getLogger(A4Service.class);
    log.info(
        String.join("\n", "",
            "                       __ __  ____  ",
            "  ____ __      _____  / // / / / /_ ",
            " / __ `/ | /| / / _ \\/ // /_/ / __ \\",
            "/ /_/ /| |/ |/ /  __/__  __/ / /_/ /",
            "\\__,_/ |__/|__/\\___/  /_/ /_/_.___/ "
        )
    );
    try {
      if (fl.root.isDirectory()) {
        this.api = new A4Api(this, fl, gson).open();
      } else {
        var cfg = A4Configs.loadFrom(fl.root.toURI().toURL(), gson);
        add(cfg);
        setActive(cfg.id);
      }
    } catch (Exception e) {
      log.error("unable to initialize load balancer service", e);
      throw new IllegalStateException(e);
    }
    return this;
  }

  public ConstraintViolations add(A4Config config) {
    var errors = A4Valid.A4ConfigVld.validate(config);
    if (errors.isEmpty()) {
      configIdx.put(config.id, config);
    }
    return errors;
  }

  @Override public void close() {
    A4Io.close(api);
    A4Io.close(instance);
  }

}
