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

  public static final char R = 'r', W = 'w', N = '-';
  public static final String
    Stop = "\uD83D\uDD34",
    Close = "✖ ", Rx = "↓ ", Tx = "↑ ", NoOp = "— ",
    Tls = "\uD83D\uDD12";

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
                   Consumer<A4TcpSess> onInit,
                   Consumer<A4TcpSess> onTearDown,
                   ExecutorService tlsExec, boolean tlsClient) {
    super(0);
    this.bkSel = Objects.requireNonNull(bkSel);
    this.tlsClient = tlsClient;
    this.tlsExec = tlsExec;
    this.onInit = Objects.requireNonNull(onInit);
    this.onTearDown = Objects.requireNonNull(onTearDown);
  }

  private void logError(Exception e) {
    if (e != null && log.isDebugEnabled()) {
      var x = rootCauseOf(e);
      log.debug(
        "{} ❌ {} - {} - {}", id,
        e.getClass().getSimpleName(),
        x.getClass().getSimpleName(),
        e == x ? e.getMessage() : format("%s - %s", e.getMessage(), x.getMessage())
      );
    }
  }

  private void tearDown(Exception e) {
    logError(e);
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
    this.id = format("%08x", format("%s-%s",
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

  private char rwBit(boolean b, char c) {
    return b ? c : N;
  }

  private boolean isClient(SelectionKey k) {
    return client.channelKey == k;
  }

  private boolean isBackend(SelectionKey k) {
    return backend != null && backend.channelKey == k;
  }

  private String stateBits(SelectionKey k) {
    var target = isClient(k) ? "c" : isBackend(k) ? "b" : N;
    return format(
      "%s %c%c %c%c",
      target,
      rwBit(client.channelKey.isReadable(), R),
      rwBit(client.channelKey.isWritable(), W),
      rwBit(backend != null && backend.channelKey.isReadable(), R),
      rwBit(backend != null && backend.channelKey.isWritable(), W)
    );
  }

  private void logState(SelectionKey k, String inOp, String outOp, int br, int bw) {
    if (log.isDebugEnabled()) {
      log.debug(
        "{} | {} {} | {} | i{} o{} | c{} b{}",
        id == null ? "????????" : id,
        inOp, outOp,
        stateBits(k),
        format("%08d", br),
        format("%08d", bw),
        client,
        backend != null ? backend : "?"
      );
    }
  }

  private void tcpUpdate(SelectionKey key, A4TcpIo in, A4TcpIo out) {
    var cl = isClient(key);
    int r = -2, w = -2;

    r = in.read();

    if (cl) {
      if (tlsClient && this.backend == null) {
        initBackend();
        logState(key, Tls, NoOp, r, w);
        return;
      }
    } else {
      bkSel.contextOf(backend.backend).trackRxTx(true, r);
    }

    if (r == -1) { // flush buffers, tear down
      r = client.writeTo(backend.channel);
      w = backend.writeTo(client.channel);
      client.writeEmpty();
      backend.writeEmpty();
      logState(key, Close, Close, r, w);
      tearDown(null);
      return;
    }

    String inOp = NoOp, outOp = NoOp;

    if (in.available()) {
      inOp = Rx;
      out.writeable(true);
      if (in.stalling()) {
        in.readable(false);
        outOp = Stop;
      }
    }

    if (in.writeable()) {
      w = out.writeTo(in);
      if (!cl) {
        bkSel.contextOf(backend.backend).trackRxTx(false, w);
      }
      outOp = w > 0 ? Tx : outOp;
      in.writeable(out.available());
      if (!out.stalling()) {
        out.readable(true);
      }
    }

    logState(key, inOp, outOp, r, w);
  }

  private void tcpUpdate(SelectionKey key) {
    if (isClient(key)) {
      tcpUpdate(key, client, backend);
    } else if (isBackend(key)) {
      tcpUpdate(key, backend, client);
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
