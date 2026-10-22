package io.vacco.a4lb.udp;

import io.vacco.a4lb.cfg.A4Backend;
import io.vacco.a4lb.cfg.A4Server;
import io.vacco.a4lb.impl.A4Srv;
import io.vacco.a4lb.sel.A4Selector;
import io.vacco.a4lb.util.A4Io;
import io.vacco.a4lb.util.A4TtlMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

import static io.vacco.a4lb.util.A4Logging.onError;

public class A4UdpSrv implements A4Srv {

  public static final Logger log = LoggerFactory.getLogger(A4UdpSrv.class);

  private final Selector selector;
  private final DatagramChannel channel;
  private final A4Server srvConfig;
  private final A4Selector bkSel;
  private final ByteBuffer buff;

  private final A4TtlMap<String, A4UdpIo> sessions;

  public A4UdpSrv(Selector selector, A4Server srv, A4Selector bkSel) {
    try {
      this.sessions = new A4TtlMap<>(srv.udp.maxSessions, A4Io::close);
      this.buff = ByteBuffer.allocateDirect(srv.udp.bufferSize);
      this.selector = Objects.requireNonNull(selector);
      this.channel = DatagramChannel.open();
      this.channel.bind(new InetSocketAddress(srv.addr.host, srv.addr.port));
      this.channel.configureBlocking(false);
      this.channel.register(selector, SelectionKey.OP_READ);
      this.bkSel = Objects.requireNonNull(bkSel);
      this.srvConfig = Objects.requireNonNull(srv);
      log.info("{} - {} - UDP ingress open", srv.id, this.channel.socket());
    } catch (IOException ioe) {
      log.error("Unable to open datagram channel {}", srv.addr, ioe);
      throw new IllegalStateException(ioe);
    }
  }

  private String idOf(InetSocketAddress address) {
    return String.format("%s:%s", address.getHostString(), address.getPort());
  }

  private A4UdpIo sessionOf(String sessId, InetSocketAddress from) {
    var timeoutMs = srvConfig.udp.idleTimeoutMs;
    var sess = sessions.get(sessId);
    if (sess != null && !sessions.refresh(sessId, timeoutMs)) {
      A4Io.close(sess);
      sess = null;
    }
    if (sess == null) {
      sess = bkSel.assign(selector, from);
      sessions.put(sessId, sess, timeoutMs);
    }
    return sess;
  }

  private void onClientPacket(InetSocketAddress from, int bytes) {
    var sessId = idOf(from);
    A4UdpIo sess;
    try {
      sess = sessionOf(sessId, from);
    } catch (Exception e) {
      onError(log, "{} - unable to assign UDP backend", e, srvConfig.id);
      return;
    }
    try {
      if (sess.backend != null) {
        bkSel.contextOf(sess.backend).trackRxTx(false, bytes);
      }
      sess.channel.write(buff.flip());
    } catch (Exception e) {
      onError(log, "{} - UDP client -> backend write failed", e, srvConfig.id);
      closeSession(sess);
    }
  }

  private void onBackendPacket(SelectionKey key, int bytes) {
    var sess = (A4UdpIo) key.attachment();
    if (sess == null) {
      return;
    }
    if (!sessions.refresh(idOf(sess.client), srvConfig.udp.idleTimeoutMs)) {
      A4Io.close(sess);
      return;
    }
    try {
      if (sess.backend != null) {
        bkSel.contextOf(sess.backend).trackRxTx(true, bytes);
      }
      this.channel.send(buff.flip(), sess.client);
    } catch (Exception e) {
      onError(log, "{} - UDP backend -> client write failed", e, srvConfig.id);
      closeSession(sess);
    }
  }

  private void onUdpRead(SelectionKey key) {
    try {
      buff.clear();
      var from = (InetSocketAddress) ((DatagramChannel) key.channel()).receive(this.buff);
      if (from == null) {
        return;
      }
      var bytes = buff.position();
      if (key.channel() == this.channel) {
        onClientPacket(from, bytes);
      } else {
        onBackendPacket(key, bytes);
      }
    } catch (Exception e) {
      var sess = (A4UdpIo) key.attachment();
      if (sess != null) {
        closeSession(sess);
      }
      onError(log, "{} - UDP update error", e, srvConfig.id);
    }
  }

  private void update() {
    A4Io.select(selector, key -> {
      if (key.isReadable()) {
        onUdpRead(key);
      }
    });
  }

  private void closeSession(A4UdpIo sess) {
    if (sess == null) {
      return;
    }
    sessions.remove(idOf(sess.client), sess);
    A4Io.close(sess);
  }

  public void closeSessionsFor(A4Backend backend) {
    if (backend == null) {
      return;
    }
    for (var e : sessions.values()) {
      var sess = e.getEntry();
      if (backend.equals(sess.backend)) {
        closeSession(sess);
      }
    }
  }

  public void retainBackends(Collection<A4Backend> keep) {
    var keepSet = new HashSet<>(keep);
    for (var e : sessions.values()) {
      var sess = e.getEntry();
      if (!keepSet.contains(sess.backend)) {
        closeSession(sess);
      }
    }
  }

  public Callable<List<A4UdpIo>> createSessionCleanupTask() {
    return () -> sessions.removeExpired(System.currentTimeMillis());
  }

  @Override
  public Void call() {
    while (true) {
      update();
    }
  }

  @Override
  public void close() {
    A4Io.close(channel);
    sessions.clear();
    A4Io.close(selector);
    log.info("{} - {} - UDP ingress closed", srvConfig.id, this.channel.socket());
  }

}
