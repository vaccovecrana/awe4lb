package io.vacco.a4lb;

import org.slf4j.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;

public class A4TcpSrv {

  private static final Logger log = LoggerFactory.getLogger(A4TcpSrv.class);

  private final ServerSocketChannel channel;
  private final Selector selector;

  public A4TcpSrv(Selector selector, InetSocketAddress address) {
    // TODO configuration parameters for all these
    // TODO this needs to accept TLS configuration parameters as well.
    try {
      this.selector = Objects.requireNonNull(selector);
      this.channel = ServerSocketChannel.open();
      this.channel.bind(address);
      this.channel.configureBlocking(false);
      this.channel.register(selector, SelectionKey.OP_ACCEPT);
      log.info("{} - Ingress open", this.channel.socket());
    } catch (IOException ioe) {
      log.error("Unable to open server socket channel {}", address, ioe);
      throw new IllegalStateException(ioe);
    }
  }

  // TODO
  //   pass in some sort of configuration object that implements gobetween's backend selection strategies
  //   https://gobetween.io/documentation.html#Balancing

  // InetSocketAddress dummy = new InetSocketAddress("websdr.ewi.utwente.nl", 8901);
  // InetSocketAddress dummy = new InetSocketAddress("172.16.3.233", 9096);
  InetSocketAddress dummy = new InetSocketAddress("0.0.0.0", 6900);

  private void initSession() {
    SocketChannel client = null;
    try {
      // TODO more config params...
      // TODO check for connection limits here.
      var cl = new A4TcpCl(channel, selector);
      var bk = new A4TcpBk(dummy, this.selector, 8192);
      new A4TcpSess(this, cl, bk);
    } catch (Exception ioe) {
      log.error("{} - Unable to initialize tcp session", channel.socket(), ioe);
      A4Io.close(client);
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
