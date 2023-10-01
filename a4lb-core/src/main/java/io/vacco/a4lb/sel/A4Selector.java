package io.vacco.a4lb.sel;

import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.tcp.A4TcpIo;
import io.vacco.a4lb.util.*;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class A4Selector {

  private final A4Match[] cfg;
  private final Map<A4Pool, ReentrantLock> poolLockIdx = new HashMap<>();
  private final Map<A4Backend, Integer> bkConnIdx = new HashMap<>();
  private final Map<A4Backend, A4RxTx> bkRxTxIdx = new HashMap<>();

  public A4Selector(A4Match[] cfg) {
    this.cfg = Objects.requireNonNull(cfg);
    Arrays.stream(cfg)
        .map(m -> m.pool)
        .forEach(p -> poolLockIdx.put(p, new ReentrantLock()));
  }

  public A4Backend select(A4Pool pool, int clientIpHash) {
    if (pool.type == null) {
      return A4SelStd.select(pool);
    }
    switch (pool.type) {
      case Weight: return A4SelWeight.select(pool);
      case RoundRobin: return A4SelRRobin.select(pool);
      case IpHash: return A4SelIpHash.select(pool, clientIpHash);
      case LeastConn: return A4SelLConn.select(pool, this);
      default: throw new IllegalStateException("Invalid pool type: " + pool.type);
    }
  }

  public Optional<A4Pool> matches(SocketChannel client, String tlsSni) {
    var clientIp = client.socket().getInetAddress().getHostAddress();
    return A4MatchOps.eval(tlsSni, clientIp, cfg);
  }

  public int connCountOf(A4Backend bk) {
    return bkConnIdx.computeIfAbsent(bk, bk0 -> 0);
  }

  public void trackConn(A4Backend bk, boolean isConnect) {
    int connections = bkConnIdx.computeIfAbsent(bk, bk0 -> 0);
    bkConnIdx.put(bk, isConnect ? connections + 1 : connections - 1);
  }

  public void trackRxTx(A4Backend bk, boolean isRx, int bytes) {
    var rxTx = bkRxTxIdx.computeIfAbsent(bk, bk0 -> new A4RxTx());
    if (isRx) {
      rxTx.updateRx(bytes);
    } else {
      rxTx.updateTx(bytes);
    }
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
    var pl = poolLockIdx.get(pool);
    pl.lock();
    try {
      return then.get();
    } finally {
      pl.unlock();
    }
  }

}
