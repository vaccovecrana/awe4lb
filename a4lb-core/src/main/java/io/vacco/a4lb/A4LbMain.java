package io.vacco.a4lb;

import com.google.gson.Gson;
import io.vacco.a4lb.util.A4Configs;
import org.slf4j.*;
import java.io.File;
import java.net.URL;

public class A4LbMain {

  private static final Logger log = LoggerFactory.getLogger(A4LbMain.class);

  public static A4Lb init(URL cfgUrl, Gson g) {
    var cfg = A4Configs.loadFrom(cfgUrl, g);
    return new A4Lb(cfg, g).start();
  }

  public static void main(String[] args) {
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
      a4lb = init(cfgFile.toURI().toURL(), new Gson());
    } catch (Exception e) {
      log.error("Unable to initialize load balancer", e);
      if (a4lb != null) {
        a4lb.stop();
      }
    }
  }


}
