package io.vacco.a4lb.tcp;

import io.vacco.a4lb.cfg.A4Match;
import io.vacco.a4lb.util.A4MatchOps;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Objects;

public class A4TcpMatch {

  private final A4Match[] cfg;

  public A4TcpMatch(A4Match[] cfg) {
    this.cfg = Objects.requireNonNull(cfg);
  }

  public A4TcpIo get(Selector selector, SocketChannel client, String tlsSni) {
    var clientHost = client.socket().getInetAddress().getHostAddress();
    var pool = A4MatchOps.eval(tlsSni, clientHost, cfg);
    if (pool.isPresent()) {
      var bk0 = pool.get().hosts[0]; // TODO implement backend selection strategies here.
      return new A4TcpIo(new InetSocketAddress(bk0.addr.host, bk0.addr.port), selector);
    }
    throw new IllegalStateException(
        String.format("No backend rule matches for [host: %s, sni: %s]", clientHost, tlsSni)
    );
  }

}
