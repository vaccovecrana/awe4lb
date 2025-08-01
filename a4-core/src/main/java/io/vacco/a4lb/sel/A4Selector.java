package io.vacco.a4lb.sel;

import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.tcp.A4TcpIo;
import io.vacco.a4lb.udp.A4UdpIo;
import io.vacco.a4lb.util.*;
import java.net.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class A4Selector {

  private final List<A4Match> cfg;
  private final Map<A4Pool, A4PoolContext> poolContextIdx = new ConcurrentHashMap<>();
  private final Map<A4Backend, A4BackendContext> bkContextIdx = new ConcurrentHashMap<>();

  public A4Selector(List<A4Match> cfg) {
    this.cfg = Objects.requireNonNull(cfg);
    cfg.stream()
      .map(m -> m.pool)
      .forEach(p -> poolContextIdx.put(p, new A4PoolContext()));
  }

  public A4Backend select(A4Pool pool, int clientIpHash) {
    var poolCtx = poolContextIdx.computeIfAbsent(pool, p -> new A4PoolContext());
    if (pool.type == null) {
      return A4SelRandom.select(pool, poolCtx);
    }
    return switch (pool.type) {
      case weight -> A4SelWeight.select(pool, poolCtx);
      case roundRobin -> A4SelRRobin.select(pool, poolCtx);
      case ipHash -> A4SelIpHash.select(pool, clientIpHash);
      case leastConn -> A4SelLConn.select(pool, bkContextIdx);
    };
  }

  public Optional<A4Match> matches(String hostAddress, String tlsSni) {
    return A4MatchOps.eval(tlsSni, hostAddress, cfg);
  }

  public Optional<A4Match> matches(Socket client, String tlsSni) {
    return matches(client.getInetAddress().getHostAddress(), tlsSni);
  }

  public A4TcpIo assign(Socket client, String tlsSni) {
    var clientAddr = client.getInetAddress();
    var clientIp = clientAddr.getHostAddress();
    try {
      var match = matches(clientIp, tlsSni).orElseThrow();
      var bk = select(match.pool, clientIp.hashCode());
      var addr = new InetSocketAddress(bk.addr.host, bk.addr.port);
      var openTls = match.tls != null && match.tls.open != null && match.tls.open;
      return new A4TcpIo(addr, openTls).backend(bk);
    } catch (Exception e) {
      throw new A4Exceptions.A4SelectException(clientIp, tlsSni, this.cfg, e);
    }
  }

  public A4UdpIo assign(Selector selector, InetSocketAddress client) {
    try {
      var match = matches(client.getHostString(), null).orElseThrow();
      var bk = select(match.pool, client.hashCode());
      return new A4UdpIo(selector, bk, client);
    } catch (Exception e) {
      throw new A4Exceptions.A4SelectException(client.toString(), null, this.cfg, e);
    }
  }

  public A4BackendContext contextOf(A4Backend bk) {
    return bkContextIdx.computeIfAbsent(bk, bk0 -> new A4BackendContext());
  }

}
