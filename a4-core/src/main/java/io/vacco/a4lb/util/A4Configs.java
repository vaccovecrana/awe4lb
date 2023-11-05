package io.vacco.a4lb.util;

import am.ik.yavi.core.ConstraintViolations;
import com.google.gson.Gson;
import io.vacco.a4lb.cfg.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

public class A4Configs {

  public static final String ExtJson = ".json";

  public static File configFileOf(File configRoot, String id) {
    return new File(configRoot, format("%s%s", id, ExtJson));
  }

  public static A4Config loadFromOrFail(URL src, Gson g) {
    try (var is = src.openStream()) {
      var isr = new InputStreamReader(is);
      return g.fromJson(isr, A4Config.class);
    } catch (IOException ioe) {
      throw new IllegalStateException("unable to load configuration from " + src, ioe);
    }
  }

  public static A4Config loadFromOrFail(File f, Gson g) {
    try {
      return loadFromOrFail(f.toURI().toURL(), g);
    } catch (IOException ioe) {
      throw new IllegalStateException("unable to load configuration from file " + f, ioe);
    }
  }

  public static A4Config syncFs(File configRoot, Gson g, A4Config config, boolean markActive) {
    var cfgFile = configFileOf(configRoot, config.id);
    config.active = markActive;
    try (var fw = new FileWriter(cfgFile)) {
      g.toJson(config, fw);
      return config;
    } catch (Exception e) {
      throw new IllegalStateException("Unable to write configuration: " + cfgFile.getAbsolutePath(), e);
    }
  }

  public static Stream<A4Config> configList(File configRoot, Gson g) {
    var files = configRoot.listFiles();
    if (files != null) {
      return Arrays.stream(files)
          .filter(f -> f.getName().endsWith(ExtJson))
          .map(f -> {
            try {
              return A4Configs.loadFromOrFail(f.toURI().toURL(), g);
            } catch (Exception e) {
              return null;
            }
          })
          .filter(Objects::nonNull);
    }
    return Stream.empty();
  }

  public static ConstraintViolations save(File configRoot, Gson g, A4Config config) {
    var errors = A4Valid.validate(config);
    if (errors.isEmpty()) {
      syncFs(configRoot, g, config, false);
    }
    return errors;
  }

  public static boolean delete(File configRoot, String configId) {
    var cfgFile = configFileOf(configRoot, configId);
    if (cfgFile.exists() && cfgFile.isFile()) {
      return cfgFile.delete();
    } else {
      return false;
    }
  }

  public static List<A4Pool> allPoolsOf(A4Server srv) {
    return srv.match.stream().map(m -> m.pool).collect(Collectors.toList());
  }

  public static List<A4Match> allMatchesOf(A4Server srv) {
    return srv.match == null ? Collections.emptyList() : srv.match;
  }

}
