package io.vacco.a4lb.util;

import io.vacco.a4lb.cfg.A4Sock;
import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

public class A4Flags {

  public static final String DefaultHost = "127.0.0.1";
  public static final int    DefaultPort = 7070;

  public File root;
  public A4Sock api;

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

    fl.root = new File(argIdx.get("config"));
    fl.api = new A4Sock()
        .host(host == null ? DefaultHost : host)
        .port(port == null ? DefaultPort : Integer.parseInt(port));

    var errors = A4Valid.A4FlagsVld.validate(fl);
    if (errors.isEmpty()) {
      throw new A4Exceptions.A4ConfigException(errors);
    }

    return fl;
  }

}
