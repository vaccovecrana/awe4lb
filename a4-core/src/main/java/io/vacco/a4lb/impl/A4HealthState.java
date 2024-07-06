package io.vacco.a4lb.impl;

import io.vacco.a4lb.cfg.*;
import org.buildobjects.process.*;
import org.slf4j.*;
import java.net.*;
import java.util.Arrays;

import static io.vacco.a4lb.util.A4Logging.onError;

public class A4HealthState {

  private static final Logger log = LoggerFactory.getLogger(A4HealthState.class);

  public static final String kHost = "$host", kPort = "$port";

  private static String[] setVars(String[] args, A4Backend bk) {
    if (args == null) {
      return new String[0];
    }
    var args0 = new String[args.length];
    for (int i = 0; i < args.length; i++) {
      args0[i] = args[i]
          .replace(kHost, bk.addr.host)
          .replace(kPort, Integer.toString(bk.addr.port));
    }
    return args0;
  }

  public static A4BackendState stateOfExec(String serverId, A4Backend bk, A4HealthCheck hlt) {
    ProcResult res = null;
    var args = setVars(hlt.exec.args, bk);
    var argsStr = Arrays.toString(args);
    if (log.isTraceEnabled()) {
      log.trace("{} - exec command: {}, {}", serverId, hlt.exec.command, argsStr);
    }
    try {
      res = new ProcBuilder(hlt.exec.command, args)
          .withTimeoutMillis(hlt.timeoutMs)
          .ignoreExitStatus()
          .run();
      if (res.getExitValue() != 0) {
        throw new IllegalStateException();
      }
      return A4BackendState.Up;
    } catch (Exception e) {
      var stdOut = res != null ? res.getOutputString() : "";
      var stdErr = res != null ? res.getErrorString() : "";
      var msg = (stdOut.isEmpty() ? stdErr : stdOut).trim();
      onError(
        log, "{} - host down (exec) - {} {} {} {}",
        e, serverId, bk.addr, hlt.exec.command, argsStr, msg
      );
      return A4BackendState.Down;
    }
  }

  public static A4BackendState stateOfTcp(String serverId, A4Backend bk, A4HealthCheck hlt) {
    try (var socket = new Socket()) {
      socket.connect(new InetSocketAddress(bk.addr.host, bk.addr.port), hlt.timeoutMs);
      return A4BackendState.Up;
    } catch (Exception e) {
      onError(log, "{} - {} - host down (tcp)", e, serverId, bk);
      return A4BackendState.Down;
    }
  }

  public static A4BackendState stateOf(String serverId, A4Backend bk, A4HealthCheck hlt) {
    var state = hlt.exec != null
        ? stateOfExec(serverId, bk, hlt)
        : stateOfTcp(serverId, bk, hlt);
    if (bk.state != state) {
      log.info("{} - backend state ({}): {} -> {}", serverId, hlt.exec != null ? "exec" : "tcp", bk, state);
    }
    return state;
  }

}
