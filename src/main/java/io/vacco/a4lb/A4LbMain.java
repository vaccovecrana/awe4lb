package io.vacco.a4lb;

import com.google.gson.Gson;
import io.vacco.a4lb.util.*;
import org.slf4j.*;

public class A4LbMain {

  private static final Logger log = LoggerFactory.getLogger(A4LbMain.class);

  public static A4Lb init(Gson g, A4Flags fl) {
    var cfg = A4Configs.loadFrom(cfgUrl, g);
    return new A4Lb(cfg, g).start();
  }

  public static String usage() {
    return String.join("\n",
        "Usage:",
        "  awe4lb [flags]",
        "Flags:",
        "  --config=string    Path to configuration root.",
        "                     A path to a file starts a single LB instance.",
        "                     A path to a directory starts a management API and a UI.",
        "  --api-host=string  API/UI host IP address. Default " + A4Flags.DefaultHost,
        "  --api-port=number  API/UI host port. Default " + A4Flags.DefaultPort
    );
  }

  public static void main(String[] args) {
    if (args == null || args.length == 0) {
      System.out.println(usage());
      return;
    }
    A4Lb a4lb = null;
    try {
      var fl = A4Flags.from(args);
      log.info(
          String.join("\n", "",
              "                       __ __  ____  ",
              "  ____ __      _____  / // / / / /_ ",
              " / __ `/ | /| / / _ \\/ // /_/ / __ \\",
              "/ /_/ /| |/ |/ /  __/__  __/ / /_/ /",
              "\\__,_/ |__/|__/\\___/  /_/ /_/_.___/ "
          )
      );
      if (fl.root.isDirectory()) { // start in UI mode

      } else { // start single lb instance

      }
      a4lb = init(fl.root.toURI().toURL(), new Gson());
    } catch (Exception e) {
      log.error("Unable to initialize load balancer", e);
      if (a4lb != null) {
        a4lb.stop();
      }
    }
  }

}
