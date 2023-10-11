package io.vacco.a4lb.impl;

import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.util.A4Exceptions;
import org.buildobjects.process.*;
import org.slf4j.*;
import java.net.*;
import java.util.Arrays;

public class A4Health {

  private static final Logger log = LoggerFactory.getLogger(A4Health.class);

  public static A4Backend.State stateOfExec(String serverId, A4Backend bk, A4HealthCheck hlt) {
    ProcResult res = null;
    var args = hlt.exec.args != null
        ? new String[hlt.exec.args.length + 2]
        : new String[2];
    try {
      args[args.length - 1] = Integer.toString(bk.addr.port);
      args[args.length - 2] = bk.addr.host;
      if (hlt.exec.args != null) {
        System.arraycopy(hlt.exec.args, 0, args, 0, hlt.exec.args.length);
      }
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
        var argsStr = Arrays.toString(args);
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
    return hlt.exec != null
        ? stateOfExec(serverId, bk, hlt)
        : stateOfTcp(serverId, bk, hlt);
  }

}
