package io.vacco.a4lb.tcp;

import io.vacco.a4lb.cfg.A4Server;
import io.vacco.a4lb.niossl.*;
import io.vacco.a4lb.sel.A4Selector;
import io.vacco.a4lb.util.A4Io;
import org.slf4j.*;
import javax.net.ssl.SSLContext;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;

public class A4TcpSrv implements Callable<Void>, Closeable {

  public static final Logger log = LoggerFactory.getLogger(A4TcpSrv.class);

  private final Selector selector;
  private final SSLContext sslContext;
  private final ServerSocketChannel channel;
  private final ExecutorService tlsExec;

  private final A4Server srvConfig;
  private final A4Selector bkSel;

  public A4TcpSrv(Selector selector, A4Server srv, A4Selector bkSel, ExecutorService tlsExec) {
    try {
      this.selector = Objects.requireNonNull(selector);
      this.channel = ServerSocketChannel.open();
      this.channel.bind(new InetSocketAddress(srv.addr.host, srv.addr.port));
      this.channel.configureBlocking(false);
      this.channel.register(selector, SelectionKey.OP_ACCEPT);
      this.bkSel = Objects.requireNonNull(bkSel);
      this.srvConfig = Objects.requireNonNull(srv);
      if (srv.tls != null) {
        log.info("{} - initializing SSL context", srv.id);
        this.sslContext = SSLCertificates.contextFrom(srv.tls);
        this.tlsExec = Objects.requireNonNull(tlsExec);
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

  private void initSession() {
    SocketChannel clientChannel = null;
    SelectionKey clientKey;
    try {
      // TODO check for connection limits here.
      var sess = new A4TcpSess(this, this.bkSel, sslContext != null, tlsExec);
      if (sslContext != null) {
        clientChannel = new SSLServerSocketChannel(
            this.channel, sslContext, tlsExec, sess,
            srvConfig.tls.protocols, srvConfig.tls.ciphers
        ).accept();
        var sslChann = (SSLSocketChannel) clientChannel;
        clientKey = sslChann.getWrappedSocketChannel().register(selector, SelectionKey.OP_READ);
      } else {
        clientChannel = this.channel.accept();
        clientChannel.configureBlocking(false);
        clientKey = clientChannel.register(selector, SelectionKey.OP_READ);
      }
      sess.setClient(new A4TcpIo(clientKey, clientChannel));
    } catch (Exception ioe) {
      log.error("{} - Unable to initialize tcp session", channel.socket(), ioe);
      if (clientChannel != null) {
        A4Io.close(clientChannel);
      }
    }
  }

  public void update() {
    A4Io.select(selector, key -> {
      if (key.channel() == this.channel && key.isAcceptable()) {
        initSession();
      } else if (key.attachment() instanceof A4TcpSess) {
        var sess = (A4TcpSess) key.attachment();
        if (sess.owner == this) {
          sess.update(key);
        } // else not one of our sessions
      }
    });
  }

  @Override public Void call() {
    while (true) {
      update();
    }
  }

  @Override public void close() {
    A4Io.close(channel);
    A4Io.close(selector);
    log.info("{} - {} - TCP ingress closed", srvConfig.id, this.channel.socket());
  }

}
