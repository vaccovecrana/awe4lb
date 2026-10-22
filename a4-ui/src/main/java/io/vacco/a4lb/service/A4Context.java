package io.vacco.a4lb.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.vacco.a4lb.cfg.A4Format;
import io.vacco.a4lb.util.A4Io;
import io.vacco.a4lb.util.A4Options;
import io.vacco.a4lb.web.A4Api;

import java.io.Closeable;
import java.util.Objects;

import static io.vacco.a4lb.util.A4Configs.loadFromOrFail;
import static io.vacco.a4lb.util.A4Logging.onError;
import static io.vacco.shax.logging.ShOption.*;
import static java.lang.String.join;

public class A4Context implements Closeable {

  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
  public final A4Service service = new A4Service(gson);

  private A4Api api;

  public void init(A4Options fl) {
    setSysProp(IO_VACCO_SHAX_PRETTYPRINT, "true");
    setSysProp(IO_VACCO_SHAX_DEVMODE, fl.logFormat == A4Format.text ? "true" : "false");
    setSysProp(IO_VACCO_SHAX_LOGLEVEL, fl.logLevel.toString());
    var log = org.slf4j.LoggerFactory.getLogger(A4Service.class);
    log.info(
      join("\n", "",
        "                       __ __  ____  ",
        "  ____ __      _____  / // / / / /_ ",
        " / __ `/ | /| / / _ \\/ // /_/ / __ \\",
        "/ /_/ /| |/ |/ /  __/__  __/ / /_/ /",
        "\\__,_/ |__/|__/\\___/  /_/ /_/_.___/ "
      )
    );
    var root = Objects.requireNonNull(fl.root).getAbsoluteFile();
    if (!root.exists()) {
      throw new IllegalArgumentException("Configuration path does not exist: " + root);
    }
    if (!root.isDirectory() && !root.isFile()) {
      throw new IllegalArgumentException("Configuration path is not a file or directory: " + root);
    }
    var configRoot = root.isDirectory() ? root : root.getParentFile();
    var startup = root.isFile() ? root : null;
    try {
      this.api = new A4Api(configRoot, service, fl, gson).open();
    } catch (Exception e) {
      onError(log, "unable to initialize API/UI: {}", e, fl);
      throw new IllegalStateException(e);
    }
    if (startup != null) {
      try {
        service.setActive(loadFromOrFail(startup, gson));
      } catch (Exception e) {
        onError(log, "unable to load startup configuration; UI remains available - {}", e, startup);
      }
    }
  }

  @Override
  public void close() {
    A4Io.close(service);
    A4Io.close(api);
  }

}
