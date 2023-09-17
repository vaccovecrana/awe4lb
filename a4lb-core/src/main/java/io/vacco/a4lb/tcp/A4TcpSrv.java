package io.vacco.a4lb.tcp;

import io.vacco.a4lb.cfg.A4Server;
import io.vacco.a4lb.sel.A4Sel;
import org.slf4j.*;
import tlschannel.*;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.Callable;

public class A4TcpSrv implements Callable<Void> {

  public static final Logger log = LoggerFactory.getLogger(A4TcpSrv.class);

  private final String id;
  private final Selector selector;
  private final SSLContext sslContext;
  private final ServerSocketChannel channel;

  private final A4Server srvConfig;
  public  final A4Sel bkSelect;

  public A4TcpSrv(Selector selector, String id, A4Server srv) {
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
      } else {
        this.sslContext = null;
      }
      log.info("{} - {} - Ingress open", this.id, this.channel.socket());
    } catch (IOException ioe) {
      log.error("Unable to open server socket channel {}", srv.addr, ioe);
      throw new IllegalStateException(ioe);
    }
  }

  private void initSession() {
    SocketChannel clientChannel = null;
    try {
      // TODO check for connection limits here.
      clientChannel = this.channel.accept();
      clientChannel.configureBlocking(false);
      var sess = new A4TcpSess(this, srvConfig.bufferSize);
      var tlc = (TlsChannel) null;
      if (sslContext != null) {
        tlc = ServerTlsChannel
            .newBuilder(clientChannel, osni -> {
              osni.flatMap(A4Ssl::sniOf).ifPresent(sess::setTlsSni);
              return Optional.of(sslContext);
            })
            .withEngineFactory(sslCtx -> A4Ssl.configureServer(sslCtx, srvConfig.tls))
            .build();
      }
      sess.setClient(new A4TcpIo(selector, clientChannel, tlc));
    } catch (Exception ioe) {
      log.error("{} - Unable to initialize tcp session", channel.socket(), ioe);
      if (clientChannel != null) {
        A4Io.close(clientChannel);
      }
    }
  }

  public void update() {
    A4Io.select(selector, key -> {
      if (key.isValid()) {
        if (key.channel() == this.channel && key.isAcceptable()) {
          initSession();
        } else if (key.attachment() instanceof A4TcpSess) {
          var sess = (A4TcpSess) key.attachment();
          if (sess.owner == this) {
            sess.update(key);
          } // else not one of our sessions, pass to next server.
        }
      } else if (log.isTraceEnabled()) {
        log.trace("{} - Invalid key state", key);
      }
    });
  }

  @Override public Void call() {
    while (true) {
      update();
    }
  }

}
