package io.vacco.a4lb.tcp;

import io.vacco.a4lb.cfg.A4Server;
import io.vacco.a4lb.niossl.SSLServerSocketChannel;
import io.vacco.a4lb.niossl.SSLSocketChannel;
import io.vacco.a4lb.sel.A4Sel;
import org.slf4j.*;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class A4TcpSrv implements Callable<Void> {

  public static final Logger log = LoggerFactory.getLogger(A4TcpSrv.class);

  private final String id;
  private final Selector selector;
  private final SSLContext sslContext;
  private final ServerSocketChannel channel;
  private final ExecutorService tlsExec;

  private final A4Server srvConfig;
  public  final A4Sel bkSelect;

  public A4TcpSrv(Selector selector, String id, A4Server srv, ExecutorService tlsExec) {
    try {
      this.id = Objects.requireNonNull(id);
      this.selector = Objects.requireNonNull(selector);
      this.channel = ServerSocketChannel.open();
      this.channel.bind(new InetSocketAddress(srv.addr.host, srv.addr.port));
      this.channel.configureBlocking(false);
      this.channel.register(selector, SelectionKey.OP_ACCEPT);
      this.bkSelect = new A4Sel(srv.match);
      this.srvConfig = Objects.requireNonNull(srv);
      if (srv.tls != null) {
        log.info("{} - initializing SSL context", id);
        this.sslContext = A4Ssl.contextFrom(srv.tls);
        this.tlsExec = Objects.requireNonNull(tlsExec);
      } else {
        this.sslContext = null;
        this.tlsExec = null;
      }
      log.info("{} - {} - Ingress open", this.id, this.channel.socket());
    } catch (IOException ioe) {
      log.error("Unable to open server socket channel {}", srv.addr, ioe);
      throw new IllegalStateException(ioe);
    }
  }

  private void initSession() {
    SocketChannel clientChannel = null;
    SelectionKey clientKey = null;
    try {
      // TODO check for connection limits here.
      var sess = new A4TcpSess(this, srvConfig.bufferSize, sslContext != null, tlsExec);
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

}
