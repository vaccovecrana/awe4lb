package io.vacco.a4lb.impl;

import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.util.A4Exceptions;
import org.buildobjects.process.*;
import org.slf4j.*;
import java.net.*;
import java.util.Arrays;

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

  public static A4Backend.State stateOfExec(String serverId, A4Backend bk, A4HealthCheck hlt) {
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
      return A4Backend.State.Up;
    } catch (Exception e) {
      var stdOut = res != null ? res.getOutputString() : "";
      var stdErr = res != null ? res.getErrorString() : "";
      var msg = (stdOut.isEmpty() ? stdErr : stdOut).trim();
      if (log.isDebugEnabled()) {
        log.debug(
            "{} - host down (exec) - {} {} {} {}",
            serverId, bk.addr, hlt.exec.command, argsStr, msg, e
        );
      } else {
        log.warn(
            "{} - host down (exec) - {} {} {}",
            serverId, bk.addr, msg, A4Exceptions.messageFor(e)
        );
      }
      return A4Backend.State.Down;
    }
  }

  public static A4Backend.State stateOfTcp(String serverId, A4Backend bk, A4HealthCheck hlt) {
    try (var socket = new Socket()) {
      socket.connect(new InetSocketAddress(bk.addr.host, bk.addr.port), hlt.timeoutMs);
      return A4Backend.State.Up;
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.debug("{} - {} - host down (tcp)", serverId, bk, e);
      } else {
        log.warn("{} - {} - host down (tcp) - {}", serverId, bk.addr, A4Exceptions.messageFor(e));
      }
      return A4Backend.State.Down;
    }
  }

  public static A4Backend.State stateOf(String serverId, A4Backend bk, A4HealthCheck hlt) {
    var state = hlt.exec != null
        ? stateOfExec(serverId, bk, hlt)
        : stateOfTcp(serverId, bk, hlt);
    if (log.isTraceEnabled()) {
      log.trace("{} - backend state ({}): {} -> {}", serverId, hlt.exec != null ? "exec" : "tcp", bk, state);
    }
    return state;
  }

}
