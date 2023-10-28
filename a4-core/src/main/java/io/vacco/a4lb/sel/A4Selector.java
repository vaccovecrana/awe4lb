package io.vacco.a4lb.sel;

import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.tcp.A4TcpIo;
import io.vacco.a4lb.util.*;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

public class A4Selector {

  private final List<A4Match> cfg;
  private final Map<A4Pool, A4PoolState> poolStateIdx = new HashMap<>();
  private final Map<A4Backend, A4BackendState> bkStateIdx = new HashMap<>();

  public A4Selector(List<A4Match> cfg) {
    this.cfg = Objects.requireNonNull(cfg);
    cfg.stream()
        .map(m -> m.pool)
        .forEach(p -> poolStateIdx.put(p, new A4PoolState()));
  }

  public A4Backend select(A4Pool pool, int clientIpHash) {
    var poolState = poolStateIdx.computeIfAbsent(pool, p -> new A4PoolState());
    if (pool.type == null) {
      return A4SelRandom.select(pool, poolState);
    }
    switch (pool.type) {
      case weight: return A4SelWeight.select(pool, poolState);
      case roundRobin: return A4SelRRobin.select(pool, poolState);
      case ipHash: return A4SelIpHash.select(pool, clientIpHash);
      case leastConn: return A4SelLConn.select(pool, bkStateIdx);
      default: throw new IllegalStateException("Invalid pool type: " + pool.type);
    }
  }

  public Optional<A4Pool> matches(SocketChannel client, String tlsSni) {
    var clientIp = client.socket().getInetAddress().getHostAddress();
    return A4MatchOps.eval(tlsSni, clientIp, cfg);
  }

  public A4TcpIo assign(Selector selector, SocketChannel client, String tlsSni, ExecutorService tlsExec) {
    var clientAddr = client.socket().getInetAddress();
    var clientIp = clientAddr.getHostAddress();
    try {
      var pool = matches(client, tlsSni).orElseThrow();
      var bk = lockPoolAnd(pool, () -> select(pool, clientIp.hashCode()));
      var io = new A4TcpIo(new InetSocketAddress(bk.addr.host, bk.addr.port), selector, pool.openTls, tlsExec);
      return io.backend(bk);
    } catch (Exception e) {
      throw new A4Exceptions.A4SelectException(clientIp, tlsSni, this.cfg, e);
    }
  }

  public <T> T lockPoolAnd(A4Pool pool, Supplier<T> then) {
    var pl = poolStateIdx.get(pool);
    pl.lock.lock();
    try {
      return then.get();
    } finally {
      pl.lock.unlock();
    }
  }

  public A4BackendState stateOf(A4Backend bk) {
    return bkStateIdx.computeIfAbsent(bk, bk0 -> new A4BackendState());
  }

}
