package io.vacco.a4lb.sel;

import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.tcp.A4TcpIo;
import io.vacco.a4lb.util.*;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class A4Sel {

  private final A4Match[] cfg;
  private final Map<A4Pool, ReentrantLock> poolLockIdx = new ConcurrentHashMap<>();

  public A4Sel(A4Match[] cfg) {
    this.cfg = Objects.requireNonNull(cfg);
  }

  public A4Backend select(A4Pool pool, int clientIpHash) {
    if (pool.type == null) {
      return A4SelStd.select(pool);
    }
    switch (pool.type) {
      case Weight: return A4SelWeight.select(pool);
      case RoundRobin: return A4SelRRobin.select(pool);
      case IpHash: return A4SelIpHash.select(pool, clientIpHash);
      case LeastConn: return A4SelLConn.select(pool);
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
      var bk = select(pool, clientIp.hashCode());
      var io = new A4TcpIo(new InetSocketAddress(bk.addr.host, bk.addr.port), selector, pool.openTls, tlsExec);
      return io.backend(bk);
    } catch (Exception e) {
      throw new A4Exceptions.A4SelectException(clientIp, tlsSni, this.cfg, e);
    }
  }

  public void lockPoolAnd(A4Pool pool, Runnable then) {
    var pl = poolLockIdx.get(pool);
    pl.lock();
    try {
      then.run();
    } finally {
      pl.unlock();
    }
  }

}
