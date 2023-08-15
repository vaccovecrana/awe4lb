package io.vacco.a4lb;

import io.vacco.shax.logging.ShOption;
import java.net.InetSocketAddress;

public class NIOForwardingServer {

  static {
    ShOption.setSysProp(ShOption.IO_VACCO_SHAX_DEVMODE, "true");
    ShOption.setSysProp(ShOption.IO_VACCO_SHAX_PRETTYPRINT, "true");
    ShOption.setSysProp(ShOption.IO_VACCO_SHAX_LOGLEVEL, "trace");
  }

  public static void main(String[] args) {
    var srv = new A4TcpSrv(A4Io.osSelector(), new InetSocketAddress("0.0.0.0", 8080));
    while (true) {
      srv.update();
    }
  }
}
