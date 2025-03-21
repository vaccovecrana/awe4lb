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

  public static final String Stop = "!", Go = "^", Close = "x", RxTx = ".", Tls = "*";

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

  private void logState(boolean isClRd, boolean isClWr,
                        boolean isBkRd, boolean isBkWr,
                        String sep, Integer bytes) {
    if (log.isDebugEnabled()) {
      var op = isClRd ? "cr" : isClWr ? "cw" : isBkRd ? "br" : isBkWr ? "bw" : "??";
      log.debug(
        "{} {} {} {} cl{} bk{}",
        id == null ? "????????" : id,
        sep,
        format("%06d", bytes),
        op, client,
        backend != null ? backend : "?"
      );
    }
  }

  private void tcpUpdate(SelectionKey key) {
    var isCr = key.isReadable() && client.channelKey == key;
    var isCw = key.isWritable() && client.channelKey == key;
    var isBr = key.isReadable() && backend != null && backend.channelKey == key;
    var isBw = key.isWritable() && backend != null && backend.channelKey == key;
    var bytes = 0;

    if (isCr) { // client ready to send data
      bytes = client.read();
      if (bytes == 0 && tlsClient) { // TLS handshake complete.
        initBackend();
        logState(isCr, isCw, isBr, isBw, Tls, bytes);
        return;
      }
    }

    if (isBr) { // backend ready to send data
      bytes = backend.read();
      bkSel.contextOf(backend.backend).trackRxTx(true, bytes);
    }

    if (bytes == -1) {
      if (client.isEmpty() && backend.isEmpty()) {
        logState(isCr, isCw, isBr, isBw, Close, bytes);
        tearDown(null);
        return;
      }
    }

    if (isCr) {
      if (client.isStalling()) {
        client.channelKey.interestOps(0);
        logState(isCr, isCw, isBr, isBw, Stop, bytes);
        return;
      }
      if (!client.isEmpty()) {
        backend.channelKey.interestOps(SelectionKey.OP_WRITE);
      }
    }

    if (isBr) {
      if (backend.isStalling()) {
        backend.channelKey.interestOps(0);
        logState(isCr, isCw, isBr, isBw, Stop, bytes);
        return;
      }
      if (!backend.isEmpty()) {
        client.channelKey.interestOps(SelectionKey.OP_WRITE);
      }
    }

    if (isCw) { // client ready to receive data
      if (backend != null) {
        if (backend.isEmpty()) {
          bytes = client.writeEmpty(); // ... and now, for the tricky bit.
          client.channelKey.interestOps(SelectionKey.OP_READ);
        } else {
          bytes = backend.writeTo(client.channel);
          if (backend.channelKey.interestOps() == 0 && !backend.isStalling()) {
            backend.channelKey.interestOps(SelectionKey.OP_READ);
            logState(isCr, isCw, isBr, isBw, Go, bytes);
            return;
          }
        }
      }
    }

    if (isBw) { // backend ready to receive data
      if (client.isEmpty()) {
        bytes = backend.writeEmpty();
        backend.channelKey.interestOps(SelectionKey.OP_READ);
      } else {
        bytes = client.writeTo(backend.channel);
        bkSel.contextOf(backend.backend).trackRxTx(false, bytes);
        if (client.channelKey.interestOps() == 0 && client.isStalling()) {
          client.channelKey.interestOps(SelectionKey.OP_READ);
          logState(isCr, isCw, isBr, isBw, Go, bytes);
          return;
        }
      }
    }

    if (bytes != 0) {
      logState(isCr, isCw, isBr, isBw, RxTx, bytes);
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
