package io.vacco.a4lb;

import com.google.gson.Gson;
import io.vacco.a4lb.util.A4Configs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import java.io.File;

public class A4LbMain {

  static {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }

  private static final Logger log = LoggerFactory.getLogger(A4LbMain.class);

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
      var g = new Gson();
      var cfg = A4Configs.loadFrom(cfgFile.toURI().toURL(), g);
      a4lb = new A4Lb(cfg, g);
      a4lb.start();
    } catch (Exception e) {
      log.error("Unable to initialize load balancer", e);
      if (a4lb != null) {
        a4lb.stop();
      }
    }
  }


}
