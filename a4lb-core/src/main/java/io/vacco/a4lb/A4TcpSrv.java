package io.vacco.a4lb;

import org.slf4j.*;
import stormpot.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class A4TcpSrv {

  private static final Logger log = LoggerFactory.getLogger(A4TcpSrv.class);

  private final ServerSocketChannel channel;
  private final Selector selector;
  private final Pool<A4TcpBk> bkPool;

  private final Map<SelectionKey, A4TcpSess> clIdx = new HashMap<>();
  private final Map<SelectionKey, A4TcpSess> bkIdx = new HashMap<>();

  public A4TcpSrv(Selector selector, InetSocketAddress address) {
    // TODO configuration parameters for all these
    // TODO this needs to accept TLS configuration parameters as well.
    try {
      this.selector = Objects.requireNonNull(selector);
      this.channel = ServerSocketChannel.open();
      this.channel.bind(address);
      this.channel.configureBlocking(false);
      this.channel.register(selector, SelectionKey.OP_ACCEPT);

      var alloc = new A4TcpBkAlloc("127.0.0.1", 6900, selector, 512);
      this.bkPool = Pool.from(alloc)
          .setSize(2)
          .setExpiration(new A4TcpBkExp())
          .build();
      log.info("{} - Ingress open", this.channel.socket());
    } catch (IOException ioe) {
      log.error("Unable to open server socket channel {}", address, ioe);
      throw new IllegalStateException(ioe);
    }
  }

  private void initSession() {
    SocketChannel client = null;
    try { // TODO more config params...
      client = channel.accept();
      client.configureBlocking(false);
      var clientKey = client.register(selector, SelectionKey.OP_READ);
      var to = new Timeout(2, TimeUnit.SECONDS);
      var bk = bkPool.claim(to);
      var sess = new A4TcpSess(client, bk, () -> {
        clIdx.remove(clientKey);
        bkIdx.remove(bk.channelKey);
      });
      clIdx.put(clientKey, sess);
      bkIdx.put(bk.channelKey, sess);
    } catch (Exception ioe) {
      log.error("{} - Unable to initialize tcp session", channel.socket(), ioe);
      A4Io.close(client);
    }
  }

  public void update() { // TODO document that this needs to run in a dedicated thread.
    A4Io.select(selector, key -> {
      if (key.channel() == this.channel && key.isAcceptable()) {
        initSession();
      } else if (clIdx.containsKey(key)) {
        clIdx.get(key).update(key);
      } else if (bkIdx.containsKey(key)) {
        bkIdx.get(key).update(key);
      } // else no match, should this be logged?
    });
  }

}
