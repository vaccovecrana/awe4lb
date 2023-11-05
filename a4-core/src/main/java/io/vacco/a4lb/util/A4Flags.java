package io.vacco.a4lb.util;

import io.vacco.a4lb.cfg.A4Format;
import io.vacco.a4lb.cfg.A4Sock;
import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

public class A4Flags {

  public enum LogLevel { error, warning, info, debug, trace }

  public static final String DefaultHost = "127.0.0.1";
  public static final int    DefaultPort = 7070;

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
        "  awe4lb [flags]",
        "Flags:",
        "  --config=string      Configuration path.",
        "                       A file starts a single load balancer instance.",
        "                       A Directory starts the API/UI to manage multiple configurations.",
        "  --api-host=string    API/UI host IP address. Default: " + A4Flags.DefaultHost,
        "  --api-port=number    API/UI host port. Default: " + A4Flags.DefaultPort,
        "  --log-format=string  Log output format ('text' or 'json'). Default: " + A4Format.text,
        "  --log-level=string   Log level ('error', 'warning', 'info', 'debug', 'trace'). Default: info"
    );
  }

  public static String flagOf(String key, String value) {
    return String.format("%s=%s", key, value);
  }

  public static A4Flags from(String[] args) {
    var argIdx = Arrays.stream(args)
        .filter(arg -> arg.startsWith("--"))
        .map(arg -> arg.split("="))
        .filter(pair -> pair.length == 2)
        .collect(Collectors.toMap(pair -> pair[0], pair -> pair[1]));
    var fl = new A4Flags();
    var host = argIdx.get(kApiHost);
    var port = argIdx.get(kApiPort);
    var logFormat = argIdx.get(kLogFormat);
    var logLevel = argIdx.get(kLogLevel);

    fl.root = new File(argIdx.get(kConfig));
    fl.logFormat = logFormat != null ? A4Format.valueOf(logFormat) : fl.logFormat;
    fl.logLevel = logLevel != null ? LogLevel.valueOf(logLevel) : fl.logLevel;
    fl.api = new A4Sock()
        .host(host == null ? DefaultHost : host)
        .port(port == null ? DefaultPort : Integer.parseInt(port));

    return A4Valid.validateOrFail(fl);
  }

}
