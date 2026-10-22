package io.vacco.a4lb.tcp;

import io.vacco.a4lb.cfg.A4StatsServer;
import io.vacco.a4lb.cfg.A4Tcp;
import io.vacco.a4lb.sel.A4Selector;
import io.vacco.a4lb.util.A4Io;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import static io.vacco.a4lb.util.A4Logging.onError;

/**
 * Per-listener admission and lifecycle tracking for TCP sessions.
 *
 * <p>Only per-source connection limits are enforced here. Established sessions
 * deliberately have no idle or lifetime timeout: long-lived streaming clients
 * may pause for extended periods between bursts.</p>
 */
public class A4TcpSessions {

  private static final Logger log = LoggerFactory.getLogger(A4TcpSessions.class);
  private static final long RejectLogIntervalMs = 1000;

  public static final class Lease implements AutoCloseable {
    private final AtomicBoolean released = new AtomicBoolean();
    private final Runnable onRelease;

    Lease(Runnable onRelease) {
      this.onRelease = Objects.requireNonNull(onRelease);
    }

    public void release() {
      if (released.compareAndSet(false, true)) {
        onRelease.run();
      }
    }

    @Override
    public void close() {
      release();
    }
  }

  public record SessionContext(A4TcpSessions owner, long id, Lease lease, boolean tls) {
  }

  private final String serverId;
  private final A4Tcp config;
  private final Map<Long, A4TcpSess> sessions = new ConcurrentHashMap<>();
  private final Map<InetAddress, AtomicInteger> perIp = new ConcurrentHashMap<>();
  private final AtomicLong nextSessionId = new AtomicLong();
  private final AtomicInteger activeHandshakes = new AtomicInteger();
  private final LongAdder rejectedConnections = new LongAdder();
  private final LongAdder rejectedTlsHandshakes = new LongAdder();
  private final LongAdder tlsHandshakeTimeouts = new LongAdder();
  private final AtomicLong lastRejectLogMs = new AtomicLong();
  private final AtomicLong lastHandshakeRejectLogMs = new AtomicLong();
  private final AtomicLong lastHandshakeTimeoutLogMs = new AtomicLong();
  private volatile boolean closed;

  public A4TcpSessions(String serverId, A4Tcp config) {
    this.serverId = Objects.requireNonNull(serverId);
    this.config = Objects.requireNonNull(config);
  }

  private void releasePerIp(InetAddress client) {
    perIp.compute(client, (key, count) -> {
      if (count == null) {
        return null;
      }
      return count.decrementAndGet() <= 0 ? null : count;
    });
  }

  private void logReject(InetAddress client) {
    var now = System.currentTimeMillis();
    var previous = lastRejectLogMs.get();
    if (now - previous >= RejectLogIntervalMs && lastRejectLogMs.compareAndSet(previous, now)) {
      log.warn(
        "{} - rejecting TCP connection from {} - per-IP limit {} ({} rejected)",
        serverId, client, config.maxConnectionsPerIp, rejectedConnections.sum()
      );
    }
  }

  private void logHandshakeReject() {
    var now = System.currentTimeMillis();
    var previous = lastHandshakeRejectLogMs.get();
    if (now - previous >= RejectLogIntervalMs && lastHandshakeRejectLogMs.compareAndSet(previous, now)) {
      log.warn(
        "{} - rejecting TLS handshake - concurrent limit {} ({} rejected)",
        serverId, config.maxTlsHandshakes, rejectedTlsHandshakes.sum()
      );
    }
  }

  private void logHandshakeTimeout() {
    var now = System.currentTimeMillis();
    var previous = lastHandshakeTimeoutLogMs.get();
    if (now - previous >= RejectLogIntervalMs && lastHandshakeTimeoutLogMs.compareAndSet(previous, now)) {
      log.warn(
        "{} - TLS handshake timeouts ({} total)",
        serverId, tlsHandshakeTimeouts.sum()
      );
    }
  }

  private Lease acquirePerIp(InetAddress client) {
    var accepted = new AtomicBoolean(false);
    perIp.compute(client, (key, current) -> {
      var count = current != null ? current : new AtomicInteger();
      if (count.incrementAndGet() <= config.maxConnectionsPerIp) {
        accepted.set(true);
        return count;
      }
      count.decrementAndGet();
      return count.get() == 0 ? null : count;
    });
    if (!accepted.get()) {
      rejectedConnections.increment();
      logReject(client);
      return null;
    }
    return new Lease(() -> releasePerIp(client));
  }

  public A4TcpSess open(A4Selector bkSel, Socket clientSocket, boolean tls) {
    if (closed) {
      A4Io.close(clientSocket);
      return null;
    }
    var client = clientSocket.getInetAddress();
    var lease = acquirePerIp(client);
    if (lease == null) {
      A4Io.close(clientSocket);
      return null;
    }
    try {
      var id = nextSessionId.incrementAndGet();
      var sess = new A4TcpSess(bkSel, new SessionContext(this, id, lease, tls))
        .withClient(new A4TcpIo(clientSocket));
      if (tls && clientSocket instanceof SSLSocket sslSocket) {
        var params = sslSocket.getSSLParameters();
        params.setSNIMatchers(Collections.singletonList(sess));
        sslSocket.setSSLParameters(params);
      }
      sessions.put(id, sess);
      if (closed) {
        sessions.remove(id);
        A4Io.close(sess);
        return null;
      }
      return sess;
    } catch (Exception e) {
      lease.release();
      A4Io.close(clientSocket);
      onError(log, "{} - unable to initialize tcp session", e, serverId);
      return null;
    }
  }

  Lease tryAcquireHandshake() {
    if (activeHandshakes.incrementAndGet() > config.maxTlsHandshakes) {
      activeHandshakes.decrementAndGet();
      rejectedTlsHandshakes.increment();
      logHandshakeReject();
      return null;
    }
    return new Lease(this::releaseHandshake);
  }

  private void releaseHandshake() {
    activeHandshakes.decrementAndGet();
  }

  void recordTlsHandshakeTimeout() {
    tlsHandshakeTimeouts.increment();
    logHandshakeTimeout();
  }

  int tlsHandshakeTimeoutMs() {
    return config.tlsHandshakeTimeoutMs;
  }

  void unregister(long sessionId) {
    sessions.remove(sessionId);
  }

  public int closeAll() {
    closed = true;
    var count = sessions.size();
    for (var sess : new ArrayList<>(sessions.values())) {
      A4Io.close(sess);
    }
    sessions.clear();
    return count;
  }

  public A4StatsServer stats() {
    var stats = new A4StatsServer();
    stats.id = serverId;
    stats.activeConnections = sessions.size();
    stats.activeTlsHandshakes = activeHandshakes.get();
    stats.rejectedConnections = rejectedConnections.sum();
    stats.rejectedTlsHandshakes = rejectedTlsHandshakes.sum();
    stats.tlsHandshakeTimeouts = tlsHandshakeTimeouts.sum();
    return stats;
  }

}
