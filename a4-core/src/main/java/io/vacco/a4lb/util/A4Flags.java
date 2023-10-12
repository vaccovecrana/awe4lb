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

  public File root;
  public A4Sock api;
  public A4Format logFormat = A4Format.text;
  public LogLevel logLevel = LogLevel.info;

  public static String usage() {
    return String.join("\n",
        "Usage:",
        "  awe4lb [flags]",
        "Flags:",
        "  --config=string      Path to configuration root.",
        "                       A file starts a single load balancer instance.",
        "                       A directory starts a management API/UI, allowing multiple load balancer instances..",
        "  --api-host=string    API/UI host IP address. Default: " + A4Flags.DefaultHost,
        "  --api-port=number    API/UI host port. Default: " + A4Flags.DefaultPort,
        "  --log-format=string  Log output format ('text' or 'json'). Default: " + A4Format.text,
        "  --log-level=string   Log level ('error', 'warning', 'info', 'debug', 'trace'). Default: info"
    );
  }

  public static A4Flags from(String[] args) {
    var argIdx = Arrays.stream(args)
        .filter(arg -> arg.startsWith("--"))
        .map(arg -> arg.replace("--", ""))
        .map(arg -> arg.split("="))
        .filter(pair -> pair.length == 2)
        .collect(Collectors.toMap(pair -> pair[0], pair -> pair[1]));
    var fl = new A4Flags();
    var host = argIdx.get("api-host");
    var port = argIdx.get("api-port");
    var logFormat = argIdx.get("log-format");
    var logLevel = argIdx.get("log-level");

    fl.root = new File(argIdx.get("config"));
    fl.logFormat = logFormat != null ? A4Format.valueOf(logFormat) : fl.logFormat;
    fl.logLevel = logLevel != null ? LogLevel.valueOf(logLevel) : fl.logLevel;
    fl.api = new A4Sock()
        .host(host == null ? DefaultHost : host)
        .port(port == null ? DefaultPort : Integer.parseInt(port));

    var errors = A4Valid.A4FlagsVld.validate(fl);
    if (!errors.isEmpty()) {
      throw new A4Exceptions.A4ConfigException(errors);
    }

    return fl;
  }

}
