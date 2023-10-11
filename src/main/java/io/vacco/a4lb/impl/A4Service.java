package io.vacco.a4lb.impl;

import com.google.gson.Gson;
import io.vacco.a4lb.api.A4Api;
import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.util.A4Io;
import io.vacco.a4lb.util.*;
import java.io.Closeable;

import static io.vacco.shax.logging.ShOption.*;

public class A4Service implements Closeable {

  public A4Api api;
  public A4Config config;
  public A4Lb instance;
  public Gson     gson = new Gson();

  public A4Service init(A4Flags fl) {
    setSysProp(IO_VACCO_SHAX_PRETTYPRINT, "true");
    setSysProp(IO_VACCO_SHAX_DEVMODE, fl.logFormat == A4Format.text ? "true" : "false");
    setSysProp(IO_VACCO_SHAX_LOGLEVEL, fl.logLevel.toString());
    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(A4Service.class);

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
        log.info("Starting static load balancer instance");
        this.config = A4Configs.loadFrom(fl.root.toURI().toURL(), gson);
        this.instance = new A4Lb(config, gson).open();
      }
    } catch (Exception e) {
      log.error("Unable to initialize load balancer", e);
      throw new IllegalStateException(e);
    }
    return this;
  }

  @Override public void close() {
    A4Io.close(api);
    A4Io.close(instance);
  }

}
