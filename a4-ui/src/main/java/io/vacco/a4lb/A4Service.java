package io.vacco.a4lb;

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

  private Logger log;

  public A4Api              api;
  public Gson               gson = new Gson();
  public Map<String, A4Lb>  instances = new TreeMap<>();

  public void initInstance(A4Config config) {
    A4Lb lb = null;
    try {
      lb = new A4Lb(config, gson).open();
      instances.put(config.id, lb);
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.debug("{} - unable to initialize load balancer instance", config.id, e);
      } else {
        log.error("{} - unable to initialize load balancer instance - {}", config.id, e.getMessage());
      }
      A4Io.close(lb);
      instances.remove(config.id);
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
        initInstance(A4Configs.loadFrom(fl.root.toURI().toURL(), gson));
      }
    } catch (Exception e) {
      log.error("unable to initialize load balancer service", e);
      throw new IllegalStateException(e);
    }
    return this;
  }

  @Override public void close() {
    A4Io.close(api);
    instances.values().forEach(A4Io::close);
    instances.clear();
  }

}
