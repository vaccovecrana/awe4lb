package io.vacco.a4lb.tcp;

import io.vacco.a4lb.cfg.A4Server;
import org.slf4j.*;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;

public class A4TcpSrv {

  private static final Logger log = LoggerFactory.getLogger(A4TcpSrv.class);

  private final String id;
  private final SSLContext sslContext;
  private final ServerSocketChannel channel;
  private final Selector selector;

  public A4TcpSrv(Selector selector, String id, A4Server srv) {
    // TODO configuration parameters for all these
    // TODO this needs to accept TLS configuration parameters as well.
    try {
      this.id = Objects.requireNonNull(id);
      this.selector = Objects.requireNonNull(selector);
      this.channel = ServerSocketChannel.open();
      this.channel.bind(new InetSocketAddress(srv.addr.host, srv.addr.port));
      this.channel.configureBlocking(false);
      this.channel.register(selector, SelectionKey.OP_ACCEPT);
      if (srv.tls != null) {
        log.info("{} - initializing SSL context", id);
        this.sslContext = A4X509.contextFrom(new File(srv.tls.certPath), new File(srv.tls.keyPath));
        if (srv.tls.tlsVersions != null) {
          sslContext.pr
        }
      } else {
        this.sslContext = null;
      }
      log.info("{} - {} - Ingress open", this.id, this.channel.socket());
    } catch (IOException ioe) {
      log.error("Unable to open server socket channel {}", srv.addr, ioe);
      throw new IllegalStateException(ioe);
    }
  }

  // TODO
  //   pass in some sort of configuration object that implements gobetween's backend selection strategies
  //   https://gobetween.io/documentation.html#Balancing

  // InetSocketAddress dummy = new InetSocketAddress("websdr.ewi.utwente.nl", 8901);
  // InetSocketAddress dummy = new InetSocketAddress("172.16.3.233", 9096);
  InetSocketAddress dummy = new InetSocketAddress("0.0.0.0", 6900);

  // TODO how many SSL context customization options should be exposed as config parameters?
  SSLContext sslContext = A4X509.contextFrom(
      new File("/home/jjzazuet/code/awe4lb/a4lb-core/src/test/resources/cert.pem"),
      new File("/home/jjzazuet/code/awe4lb/a4lb-core/src/test/resources/key.pem")
  );

  private void initSession() {
    A4TcpIo cl = null, bk = null;
    try {
      // TODO more config params...
      // TODO check for connection limits here.
      cl = new A4TcpIo(channel, selector, sslContext);
      bk = new A4TcpIo(dummy, this.selector);
      new A4TcpSess(this, cl, bk, 8192);
    } catch (Exception ioe) {
      log.error("{} - Unable to initialize tcp session", channel.socket(), ioe);
      if (cl != null) {
        cl.close();
      }
      if (bk != null) {
        bk.close();
      }
    }
  }

  public void update() { // TODO document that this needs to run in a dedicated thread.
    A4Io.select(selector, key -> {
      if (key.channel() == this.channel && key.isAcceptable()) {
        initSession();
      } else if (key.attachment() instanceof A4TcpSess) {
        var sess = (A4TcpSess) key.attachment();
        if (sess.owner == this) {
          sess.update(key);
        } // else not one of our sessions, pass to next server.
      } else {
        log.error("{} - Invalid key state", key);
      }
    });
  }

}
