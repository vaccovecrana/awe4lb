package io.vacco.a4lb.tcp;

import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.util.*;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;

public class A4TcpMatch {

  private final A4Match[] cfg;

  public A4TcpMatch(A4Match[] cfg) {
    this.cfg = Objects.requireNonNull(cfg);
  }

  public A4Backend select(String clientHost, int clientIpHash, String tlsSni) {
    try {
      var oPool = A4MatchOps.eval(tlsSni, clientHost, cfg);
      if (oPool.isPresent()) {
        var pool = oPool.get();
        if (pool.type == null) {
          return A4TcpStd.select(pool);
        }
        switch (pool.type) {
          case Weight: return A4TcpWeight.select(pool);
          case RoundRobin: return A4TcpRRobin.select(pool);
          case IpHash: return A4TcpIpHash.select(pool, clientIpHash);
        }
      }
      throw new IllegalStateException();
    } catch (Exception e) {
      throw new A4Exceptions.A4SelectException(clientHost, tlsSni, this.cfg, e);
    }
  }

  public A4TcpIo get(Selector selector, SocketChannel client, String tlsSni) {
    var clientIp = client.socket().getInetAddress();
    var bk = select(clientIp.getHostAddress(), clientIp.hashCode(), tlsSni);
    var io = new A4TcpIo(new InetSocketAddress(bk.addr.host, bk.addr.port), selector);
    return io.target(bk);
  }

}
