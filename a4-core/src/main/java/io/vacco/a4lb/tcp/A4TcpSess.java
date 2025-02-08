package io.vacco.a4lb.tcp;

import io.vacco.a4lb.cfg.A4Match;
import io.vacco.a4lb.niossl.*;
import io.vacco.a4lb.sel.A4Selector;
import io.vacco.a4lb.util.A4Io;
import org.slf4j.*;
import javax.net.ssl.*;
import java.io.Closeable;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static io.vacco.a4lb.util.A4Logging.*;
import static java.lang.String.format;

public class A4TcpSess extends SNIMatcher implements Closeable {

  public static int MaxBackendBuffers = 4;
  public static int MaxClientBuffers = 4;

  private static final Logger log = LoggerFactory.getLogger(A4TcpSess.class);

  private A4Selector bkSel;
  private A4TcpIo client, backend;
  public  String id;

  private String tlsSni;
  private A4Match tlsMatch;
  private ExecutorService tlsExec;
  private final boolean tlsClient;

  private Consumer<A4TcpSess> onInit, onTearDown;

  public A4TcpSess(A4Selector bkSel,
                   Consumer<A4TcpSess> onInit, Consumer<A4TcpSess> onTearDown,
                   boolean tlsClient, ExecutorService tlsExec) {
    super(0);
    this.bkSel = Objects.requireNonNull(bkSel);
    this.tlsClient = tlsClient;
    this.tlsExec = tlsExec;
    this.onInit = Objects.requireNonNull(onInit);
    this.onTearDown = Objects.requireNonNull(onTearDown);
  }

  private void tearDown(Exception e) {
    if (e != null && log.isDebugEnabled()) {
      var x = rootCauseOf(e);
      log.debug(format("!! [%s, %s] - %s - %s - %s",
        client != null ? client.id : "?",
        backend != null ? backend.id : "?",
        e.getClass().getSimpleName(), x.getClass().getSimpleName(),
        e == x ? e.getMessage() : format("%s - %s", e.getMessage(), x.getMessage())
      ));
    }
    A4Io.close(client);
    A4Io.close(backend);
    if (backend != null) {
      bkSel.contextOf(backend.backend).trackConn(false);
    }
    this.onTearDown.accept(this);
    this.client = null;
    this.backend = null;
    this.bkSel = null;
    this.tlsExec = null;
    this.tlsSni = null;
    this.tlsMatch = null;
    this.id = null;
    this.onInit = null;
    this.onTearDown = null;
    if (log.isDebugEnabled()) {
      log.debug("------------------------------");
    }
  }

  private void initBackend() {
    if (this.backend != null) {
      return;
    }
    if (tlsClient && tlsSni == null) {
      return;
    }
    this.backend = bkSel.assign(client.channelKey.selector(), client.channel, tlsSni, tlsExec);
    this.backend.channelKey.attach(this);
    this.id = format("%x", format("%s-%s",
      client.getRawChannel().socket(),
      backend.getRawChannel().socket()
    ).hashCode());
    this.bkSel.contextOf(backend.backend).trackConn(true);
    this.onInit.accept(this);
  }

  @Override public boolean matches(SNIServerName sn) {
    var sni = SSLCertificates.sniOf(sn).orElseThrow();
    var op = bkSel.matches(client.channel, sni);
    if (op.isPresent()) {
      this.tlsSni = sni;
      this.tlsMatch = op.get();
      return true;
    }
    return false;
  }

  private String iv(boolean v) {
    return v ? "1" : "0";
  }

  private String logOpBitsOf(boolean isCl, boolean isClRd, boolean isClWr,
                             boolean isBk, boolean isBkRd, boolean isBkWr) {
    return format(
      "c: %s %s %s, b: %s %s %s",
      iv(isCl), iv(isClRd), iv(isClWr),
      iv(isBk), iv(isBkRd), iv(isBkWr)
    );
  }

  private String logState(Integer bytes) {
    return format(
      "%s - %s cl%s bk%s",
      id, format("%06d", bytes), client, backend != null ? backend : "?"
    );
  }

  private void tcpUpdate(SelectionKey key) {
    var isCl = client.channelKey == key;
    var isBk = backend != null && backend.channelKey == key;
    var isClRd = key.isReadable() && isCl;
    var isClWr = key.isWritable() && isCl;
    var isBkRd = key.isReadable() && isBk;
    var isBkWr = key.isWritable() && isBk;
    var bytes = (Integer) null;
    var doLog = true;

    if (isClRd) {
      bytes = client.read();
      if (client.bufferQueue.size() > MaxClientBuffers) {
        client.channelKey.interestOps(0);
        if (backend.channelKey.interestOps() == SelectionKey.OP_READ) {
          backend.channelKey.interestOps(SelectionKey.OP_WRITE);
        }
        if (log.isDebugEnabled()) {
          log.debug("{} CL STOP {}", logState(bytes), logOpBitsOf(isCl, isClRd, isClWr, isBk, isBkRd, isBkWr));
        }
        return;
      }
    } else if (isClWr) {
      bytes = backend.writeTo(client.channel);
      if (backend.channelKey.interestOps() == 0) {
        if (log.isDebugEnabled()) {
          log.debug("{} BK GO   {}", logState(bytes), logOpBitsOf(isCl, isClRd, isClWr, isBk, isBkRd, isBkWr));
        }
        backend.channelKey.interestOps(SelectionKey.OP_READ);
        doLog = false;
      }
    } else if (isBkRd) {
      bytes = backend.read();
      if (backend.bufferQueue.size() > MaxBackendBuffers) {
        backend.channelKey.interestOps(0);
        if (client.channelKey.interestOps() == SelectionKey.OP_READ) {
          client.channelKey.interestOps(SelectionKey.OP_WRITE);
        }
        if (log.isDebugEnabled()) {
          log.debug("{} BK STOP {}", logState(bytes), logOpBitsOf(isCl, isClRd, isClWr, isBk, isBkRd, isBkWr));
        }
        return;
      }
      bkSel.contextOf(backend.backend).trackRxTx(true, bytes);
    } else if (isBkWr) {
      bytes = client.writeTo(backend.channel);
      if (client.channelKey.interestOps() == 0) {
        if (log.isDebugEnabled()) {
          log.debug("{} CL GO   {}", logState(bytes), logOpBitsOf(isCl, isClRd, isClWr, isBk, isBkRd, isBkWr));
        }
        client.channelKey.interestOps(SelectionKey.OP_READ);
        doLog = false;
      }
      bkSel.contextOf(backend.backend).trackRxTx(false, bytes);
    }

    if (doLog && log.isDebugEnabled()) {
      log.debug(logState(bytes));
    }

    if (bytes != null) {
      if (bytes > 0) {
        if (isBkRd) {
          client.channelKey.interestOps(SelectionKey.OP_WRITE);
        }
        if (isBkWr) {
          backend.channelKey.interestOps(SelectionKey.OP_READ);
        }
        if (isClRd && backend != null) {
          backend.channelKey.interestOps(SelectionKey.OP_WRITE);
        }
        if (isClWr) {
          client.channelKey.interestOps(SelectionKey.OP_READ);
        }
      } else if (bytes == 0 && tlsClient) {
        initBackend();
      } else if (bytes == -1 && isBk && !backend.bufferQueue.isEmpty()) {
        client.channelKey.interestOps(SelectionKey.OP_WRITE);
        backend.channelKey.interestOps(0);
      } else if (bytes == -1) {
        tearDown(null);
      }
    }
  }

  public void update(SelectionKey key) {
    try {
      if (key.isAcceptable()) {
        initBackend();
        return;
      }
      if (key.attachment() == this) {
        tcpUpdate(key);
      }
    } catch (Exception e) {
      tearDown(e);
    }
  }

  public A4TcpSess withClient(A4TcpIo client) {
    this.client = Objects.requireNonNull(client);
    this.client.channelKey.attach(this);
    return this;
  }

  @Override public void close() {
    this.tearDown(null);
  }

  public A4Match getTlsMatch() {
    return tlsMatch;
  }

  /*
   * The Oceanview Motel and Casino is a familiar friend to me. I stayed in countless motels like it
   * while investigating A.W.E’s across the country, back in my field agent days. Those roadside motels
   * all bleed together like a dream, same and not the same, anywhere and nowhere.
   *
   * The Oceanview operates on dream-logic, and the light-switch cord leaks out to be found in the most
   * unexpected places, and sometimes, successfully encouraged to appear and act as a convenient lock
   * to keep out those not trained in dreamscape navigation.
   *
   * Even Bureau veterans can only find one key in the Motel, the key that opens the door marked with
   * the inverted Black Pyramid. The rest, the many other doors, are still mysteries to us.
   *
   * We’re all merely guests there. Even the Board. Sometimes I need to visit, just to breathe easier
   * for a while. It beats the numb sterile apartment I spend my nights in, insulated from everything
   * but myself. I guess that’s where the whiskey comes in.
   */

}
