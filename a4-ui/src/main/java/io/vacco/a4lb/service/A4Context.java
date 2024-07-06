package io.vacco.a4lb.service;

import com.google.gson.*;
import io.vacco.a4lb.cfg.A4Format;
import io.vacco.a4lb.util.*;
import io.vacco.a4lb.web.A4Api;
import org.slf4j.Logger;
import java.io.Closeable;
import java.util.*;

import static io.vacco.a4lb.util.A4Logging.onError;
import static io.vacco.a4lb.util.A4Configs.*;
import static io.vacco.shax.logging.ShOption.*;
import static java.lang.String.join;

public class A4Context implements Closeable {

  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
  public  final A4Service service = new A4Service(gson);

  private A4Api api;

  public void init(A4Options fl) {
    setSysProp(IO_VACCO_SHAX_PRETTYPRINT, "true");
    setSysProp(IO_VACCO_SHAX_DEVMODE, fl.logFormat == A4Format.text ? "true" : "false");
    setSysProp(IO_VACCO_SHAX_LOGLEVEL, fl.logLevel.toString());
    Logger log = org.slf4j.LoggerFactory.getLogger(A4Service.class);
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
      var root = Objects.requireNonNull(fl.root);
      var configRoot = root.isDirectory() ? root : root.getParentFile();
      this.api = new A4Api(configRoot, service, fl, gson).open();
      if (root.isFile()) {
        service.setActive(loadFromOrFail(root, gson));
      }
    } catch (Exception e) {
      onError(log, "unable to initialize options: {}", e, fl);
      throw new IllegalStateException(e);
    }
  }

  @Override public void close() {
    A4Io.close(service);
    A4Io.close(api);
  }

}
