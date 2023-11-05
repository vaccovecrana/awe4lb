package io.vacco.a4lb.util;

import am.ik.yavi.core.ConstraintViolations;
import com.google.gson.Gson;
import io.vacco.a4lb.cfg.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.stream.*;

import static java.lang.String.format;

public class A4Configs {

  public static final String ExtJson = ".json";

  public static File configFileOf(File configRoot, String id) {
    return new File(configRoot, format("%s%s", id, ExtJson));
  }

  public static A4Config loadFromOrFail(URL src, Gson g) {
    try (var is = src.openStream()) {
      var isr = new InputStreamReader(is);
      var cfg = g.fromJson(isr, A4Config.class);
      for (var srv : cfg.servers) {
        for (var m : allMatchesOf(srv)) {
          if (srv.udp == null && m.healthCheck == null) {
            m.healthCheck = new A4HealthCheck();
          }
          if (m.healthCheck != null) {
            m.healthCheck.intervalMs = m.healthCheck.intervalMs != null ? m.healthCheck.intervalMs : A4HealthCheck.DefaultIntervalMs;
            m.healthCheck.timeoutMs = m.healthCheck.timeoutMs != null ? m.healthCheck.timeoutMs : A4HealthCheck.DefaultTimeoutMs;
          }
          if (m.discover != null) {
            m.discover.intervalMs = m.discover.intervalMs != null ? m.discover.intervalMs : A4Disc.DefaultIntervalMs;
            m.discover.timeoutMs = m.discover.timeoutMs != null ? m.discover.timeoutMs : A4Disc.DefaultTimeoutMs;
          }
          if (m.pool == null) {
            m.pool = new A4Pool();
          }
          if (m.pool.hosts == null) {
            m.pool.hosts = new ArrayList<>();
          }
          for (var bk : m.pool.hosts) {
            bk.state = A4Backend.State.Unknown;
          }
        }
      }
      return cfg;
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
    var cfg0 = g.fromJson(g.toJson(config), A4Config.class).active(markActive);
    for (var srv : cfg0.servers) {
      for (var m : allMatchesOf(srv)) {
        if (m.discover != null) {
          m.pool.hosts = null;
          if (m.pool.type == null) {
            m.pool = null;
          }
          if (A4Disc.DefaultIntervalMs.equals(m.discover.intervalMs)
              && A4Disc.DefaultTimeoutMs.equals(m.discover.timeoutMs)) {
            m.discover.intervalMs = null;
            m.discover.timeoutMs = null;
          }
        }
        if (m.pool != null && m.pool.hosts != null) {
          for (var bk : m.pool.hosts) {
            bk.state = null;
          }
        }
        if (m.healthCheck != null
            && A4HealthCheck.DefaultIntervalMs.equals(m.healthCheck.intervalMs)
            && A4HealthCheck.DefaultTimeoutMs.equals(m.healthCheck.timeoutMs)) {
          m.healthCheck.intervalMs = null;
          m.healthCheck.timeoutMs = null;
          if (m.healthCheck.exec == null) {
            m.healthCheck = null;
          }
        }
      }
    }
    try (var fw = new FileWriter(cfgFile)) {
      g.toJson(cfg0, fw);
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
