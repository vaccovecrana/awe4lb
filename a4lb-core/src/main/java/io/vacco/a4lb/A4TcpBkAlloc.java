package io.vacco.a4lb;

import org.slf4j.*;
import stormpot.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

public class A4TcpBkAlloc implements Allocator<A4TcpBk> {

  private static final Logger log = LoggerFactory.getLogger(A4TcpBkAlloc.class);

  private final InetSocketAddress dest;
  private final Selector selector;
  private final int bufferSize;

  // TODO
  //   pass in some sort of configuration object that implements gobetween's backend selection strategies
  //   https://gobetween.io/documentation.html#Balancing

  public A4TcpBkAlloc(InetSocketAddress dest, Selector selector, int bufferSize) {
    this.dest = Objects.requireNonNull(dest);
    this.selector = Objects.requireNonNull(selector);
    this.bufferSize = bufferSize;
  }

  public A4TcpBkAlloc(String host, int port, Selector selector, int bufferSize) {
    this(new InetSocketAddress(host, port), selector, bufferSize);
  }

  @Override public A4TcpBk allocate(Slot slot) throws Exception {
    var bkc = SocketChannel.open();
    bkc.connect(this.dest);
    bkc.configureBlocking(false);
    var bkKey = bkc.register(this.selector, SelectionKey.OP_READ);
    var bk = new A4TcpBk(slot, bkc.socket().toString(), bkKey, bkc, ByteBuffer.allocateDirect(this.bufferSize));
    if (log.isDebugEnabled()) {
      log.debug("{} - allocated", bk.id);
    }
    return bk;
  }

  @Override public void deallocate(A4TcpBk pbk) {
    if (log.isDebugEnabled()) {
      log.debug("{} - deallocating", pbk.id);
    }
    A4Io.close(pbk.channel);
    pbk.buffer = null;
  }

}
