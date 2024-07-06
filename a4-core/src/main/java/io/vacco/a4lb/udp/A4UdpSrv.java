package io.vacco.a4lb.udp;

import io.vacco.a4lb.cfg.A4Server;
import io.vacco.a4lb.impl.A4Srv;
import io.vacco.a4lb.sel.A4Selector;
import io.vacco.a4lb.util.*;
import org.slf4j.*;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.Callable;

import static io.vacco.a4lb.util.A4Logging.onError;

public class A4UdpSrv implements A4Srv {

  public static final Logger log = LoggerFactory.getLogger(A4UdpSrv.class);

  private final Selector        selector;
  private final DatagramChannel channel;
  private final A4Server        srvConfig;
  private final A4Selector      bkSel;
  private final ByteBuffer      buff;

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

  private void onUdpRead(SelectionKey key) throws IOException {
    buff.clear();
    var from = (InetSocketAddress) ((DatagramChannel) key.channel()).receive(this.buff);
    if (key.channel() == this.channel) {
      var sessId = idOf(from);
      var sess = sessions.get(sessId);
      if (sess == null) {
        sess = bkSel.assign(selector, from);
        sessions.put(sessId, sess, srvConfig.udp.idleTimeoutMs);
      }
      sess.channel.write(buff.flip());
    } else {
      var sess = (A4UdpIo) key.attachment();
      this.channel.send(buff.flip(), sess.client);
    }
  }

  public void update() {
    A4Io.select(selector, key -> {
      try {
        if (key.isReadable()) {
          onUdpRead(key);
        } else {
          A4Io.sessionMismatch(key);
        }
      } catch (Exception e) {
        onError(log, "{} - UDP update error", e, srvConfig.id);
      }
    });
  }

  public Callable<List<A4UdpIo>> createSessionCleanupTask() {
    return () -> {
      var nowMs = System.currentTimeMillis();
      var expiredSessions = new ArrayList<A4UdpIo>();
      for (var e : sessions.values()) {
        if (nowMs > e.getExpireBy()) {
          var sessionId = idOf(e.getEntry().client);
          expiredSessions.add(sessions.removeAndGet(sessionId));
        }
      }
      return expiredSessions;
    };
  }

  @Override public Void call() {
    while (true) {
      update();
    }
  }

  @Override public void close() {
    A4Io.close(channel);
    A4Io.close(selector);
    for (var e : sessions.values()) {
      A4Io.close(e.getEntry());
    }
    sessions.clear();
    log.info("{} - {} - UDP ingress closed", srvConfig.id, this.channel.socket());
  }

}
