package io.vacco.a4lb.tcp;

import io.vacco.a4lb.cfg.A4Server;
import io.vacco.a4lb.impl.A4Srv;
import io.vacco.a4lb.niossl.*;
import io.vacco.a4lb.sel.A4Selector;
import io.vacco.a4lb.util.A4Io;
import org.slf4j.*;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;

import static java.lang.String.format;

public class A4TcpSrv implements A4Srv {

  public static final Logger log = LoggerFactory.getLogger(A4TcpSrv.class);

  private final SSLContext sslContext;
  private final ServerSocketChannel channel;

  private final A4Server srvConfig;
  private final A4Selector bkSel;
  private final Map<String, A4TcpSess> sessions = new ConcurrentHashMap<>();
  private final ExecutorService tlsExec;

  public A4TcpSrv(A4Server srv, A4Selector bkSel) {
    try {
      this.channel = ServerSocketChannel.open();
      this.channel.bind(new InetSocketAddress(srv.addr.host, srv.addr.port));
      this.channel.configureBlocking(true);
      this.bkSel = Objects.requireNonNull(bkSel);
      this.srvConfig = Objects.requireNonNull(srv);
      if (srvConfig.tls != null) {
        log.info("{} - initializing SSL context", srv.id);
        this.sslContext = SSLCertificates.forServer(srv);
        this.tlsExec = Executors.newCachedThreadPool(r -> new Thread(r, format("%s-tls", srv.id)));
      } else {
        this.sslContext = null;
        this.tlsExec = null;
      }
      log.info("{} - {} - TCP ingress open", srv.id, this.channel.socket());
    } catch (IOException ioe) {
      log.error("Unable to open server socket channel {}", srv.addr, ioe);
      throw new IllegalStateException(ioe);
    }
  }

  private A4TcpSess initSession() {
    SocketChannel clientChannel = null;
    try {
      // TODO check for connection limits here.
      var isTls = srvConfig.tls != null;
      var sess = new A4TcpSess(
        this.bkSel,
        s0 -> { if (Thread.currentThread().getName() != null) sessions.put(Thread.currentThread().getName(), s0); },
        s0 -> { if (Thread.currentThread().getName() != null) sessions.remove(Thread.currentThread().getName()); },
        tlsExec, isTls
      );
      if (sslContext != null) {
        clientChannel = new SSLServerSocketChannel(
          this.channel, sslContext, tlsExec, sess,
          srvConfig.tls.protocols, srvConfig.tls.ciphers
        ).accept();
      } else {
        clientChannel = this.channel.accept();
      }
      return sess.withClient(new A4TcpIo(clientChannel));
    } catch (Exception ioe) {
      log.error("{} - Unable to initialize tcp session", channel.socket(), ioe);
      if (clientChannel != null) {
        A4Io.close(clientChannel);
      }
      return null;
    }
  }

  @Override public Void call() {
    while (true) {
      var sess = initSession();
      if (sess != null) {
        sess.start();
      }
    }
  }

  @Override public void close() {
    if (this.tlsExec != null) {
      this.tlsExec.shutdownNow();
    }
    A4Io.close(channel);

    int sessCount = this.sessions.size();
    this.sessions.values().forEach(A4Io::close);
    this.sessions.clear();
    log.info(
      "{} - {} - TCP ingress closed{}",
      srvConfig.id, this.channel.socket(),
      sessCount > 0 ? " (" + sessCount + ") sessions" : ""
    );
  }

}
