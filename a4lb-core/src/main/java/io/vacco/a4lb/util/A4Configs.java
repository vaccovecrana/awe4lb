package io.vacco.a4lb.util;

import com.google.gson.Gson;
import io.vacco.a4lb.cfg.A4Config;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class A4Configs {

  public static A4Config loadFrom(URL src, Gson g) {
    try (var is = src.openStream()) {
      var isr = new InputStreamReader(is);
      return g.fromJson(isr, A4Config.class);
    } catch (IOException ioe) {
      throw new IllegalStateException("Unable to load configuration from " + src, ioe);
    }
  }

}
