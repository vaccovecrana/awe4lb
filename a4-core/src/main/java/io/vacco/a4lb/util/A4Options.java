package io.vacco.a4lb.util;

import io.vacco.a4lb.cfg.A4Format;
import io.vacco.a4lb.cfg.A4Sock;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class A4Options {

  public enum LogLevel {error, warning, info, debug, trace}

  public static final String DefaultHost = "127.0.0.1";
  public static final int DefaultPort = 7070;

  public static final String
    kConfig = "--config",
    kApiHost = "--api-host", kApiPort = "--api-port",
    kLogFormat = "--log-format", kLogLevel = "--log-level";

  public File root;
  public A4Sock api;
  public A4Format logFormat = A4Format.text;
  public LogLevel logLevel = LogLevel.info;

  public static String usage() {
    return String.join("\n",
      "Usage:",
      "  awe4lb [options]",
      "Options:",
      "  --config=string      Configuration path. Required.",
      "                       - A file starts a load balancer configuration immediately.",
      "                       - A directory starts the API/UI to select an active configuration.",
      "  --api-host=string    API/UI host IP address. Default: " + A4Options.DefaultHost,
      "  --api-port=number    API/UI host port. Default: " + A4Options.DefaultPort,
      "  --log-format=string  Log output format ('text' or 'json'). Default: " + A4Format.text,
      "  --log-level=string   Log level ('error', 'warning', 'info', 'debug', 'trace'). Default: info",
      "  --help               Prints this help message."
    );
  }

  public static String flagOf(String key, String value) {
    return String.format("%s=%s", key, value);
  }

  private static Map<String, String> argsOf(String[] args) {
    var result = new HashMap<String, String>();
    if (args == null) {
      return result;
    }
    Arrays.stream(args)
      .filter(arg -> arg != null && arg.startsWith("--"))
      .forEach(arg -> {
        var idx = arg.indexOf('=');
        if (idx > 0 && idx < arg.length() - 1) {
          result.put(arg.substring(0, idx), arg.substring(idx + 1));
        }
      });
    return result;
  }

  public static A4Options from(String[] args) {
    var argIdx = argsOf(args);
    var opt = new A4Options();
    var config = argIdx.get(kConfig);
    var host = argIdx.get(kApiHost);
    var port = argIdx.get(kApiPort);
    var logFormat = argIdx.get(kLogFormat);
    var logLevel = argIdx.get(kLogLevel);

    opt.root = config != null ? new File(config) : null;
    if (logFormat != null) {
      opt.logFormat = A4Format.valueOf(logFormat.toLowerCase(Locale.ROOT));
    }
    if (logLevel != null) {
      opt.logLevel = LogLevel.valueOf(logLevel.toLowerCase(Locale.ROOT));
    }
    opt.api = new A4Sock()
      .host(host == null ? DefaultHost : host)
      .port(port == null ? DefaultPort : Integer.parseInt(port));

    return A4Valid.validateOrFail(opt);
  }

  @Override
  public String toString() {
    return String.format(
      "[root: %s, api: %s, logFormat: %s, logLevel: %s]",
      root != null ? root.getAbsolutePath() : "?",
      api, logFormat, logLevel
    );
  }

}
