package io.vacco.a4lb.tcp;

import io.vacco.a4lb.A4Lb;
import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.util.A4MatchOps;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;

public class A4TcpMatch {

  private final A4Match[] cfg;
  private final Random r = new Random(A4Lb.Seed);

  public A4TcpMatch(A4Match[] cfg) {
    this.cfg = Objects.requireNonNull(cfg);
  }

  public A4Backend select(SocketChannel client, String tlsSni) {
    var clientHost = client.socket().getInetAddress().getHostAddress();
    var oPool = A4MatchOps.eval(tlsSni, clientHost, cfg);
    if (oPool.isPresent()) {
      var pool = oPool.get();
      if (pool.hosts.size() == 1) {
        return pool.hosts.get(0);
      } else if (pool.type == null) {
        return pool.hosts.get(r.nextInt(pool.hosts.size()));
      }
      switch (pool.type) {
        case Weight: return A4TcpWeight.wtSelect(pool, r);
      }
    }
    throw new IllegalStateException(
        String.format("No backend rule matches for [host: %s, sni: %s]", clientHost, tlsSni)
    );
  }

  public A4TcpIo get(Selector selector, SocketChannel client, String tlsSni) {
    var bk = select(client, tlsSni);
    return new A4TcpIo(new InetSocketAddress(bk.addr.host, bk.addr.port), selector);
  }

}
