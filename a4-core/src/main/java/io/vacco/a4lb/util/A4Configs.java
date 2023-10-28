package io.vacco.a4lb.util;

import com.google.gson.Gson;
import io.vacco.a4lb.cfg.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class A4Configs {

  public static A4Config loadFrom(URL src, Gson g) {
    try (var is = src.openStream()) {
      var isr = new InputStreamReader(is);
      return g.fromJson(isr, A4Config.class);
    } catch (IOException ioe) {
      throw new IllegalStateException("Unable to load configuration from " + src, ioe);
    }
  }

  public static A4Config loadFrom(File f, Gson g) {
    try {
      return loadFrom(f.toURI().toURL(), g);
    } catch (IOException ioe) {
      throw new IllegalStateException("unable to load configuration from file " + f.getAbsolutePath(), ioe);
    }
  }

  public static List<A4Pool> allPoolsOf(A4Server srv) {
    return srv.match.stream().map(m -> m.pool).collect(Collectors.toList());
  }

  public static List<A4Match> allMatchesOf(A4Server srv) {
    return srv.match == null ? Collections.emptyList() : srv.match;
  }

}
