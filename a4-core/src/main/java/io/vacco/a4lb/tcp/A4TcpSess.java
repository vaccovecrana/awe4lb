package io.vacco.a4lb.tcp;

import io.vacco.a4lb.cfg.A4Match;
import io.vacco.a4lb.sel.A4Selector;
import io.vacco.a4lb.util.A4Io;
import org.slf4j.*;
import javax.net.ssl.*;
import java.io.Closeable;
import java.util.*;
import java.util.function.Consumer;

import static io.vacco.a4lb.util.A4Logging.*;
import static java.lang.String.format;

public class A4TcpSess extends SNIMatcher implements Closeable {

  public static final String Error = "!", Close = "✖", Rx = "↓", Tx = "↑", Tls = "*";

  private static final Logger log = LoggerFactory.getLogger(A4TcpSess.class);

  private A4Selector bkSel;
  private A4TcpIo client, backend;

  private String tlsSni;
  private A4Match tlsMatch;
  private final boolean tlsClient;

  private Consumer<A4TcpSess> onInit, onTearDown;

  public A4TcpSess(A4Selector bkSel,
                   Consumer<A4TcpSess> onInit,
                   Consumer<A4TcpSess> onTearDown,
                   boolean tlsClient) {
    super(0);
    this.bkSel = Objects.requireNonNull(bkSel);
    this.tlsClient = tlsClient;
    this.onInit = Objects.requireNonNull(onInit);
    this.onTearDown = Objects.requireNonNull(onTearDown);
  }

  private void logError(Exception e) {
    if (e != null && log.isDebugEnabled()) {
      var x = rootCauseOf(e);
      log.debug(
        "{} | {} | {} - {} - {}",
        Thread.currentThread().getName(),
        Error,
        e.getClass().getSimpleName(),
        x.getClass().getSimpleName(),
        e == x ? e.getMessage() : format("%s - %s", e.getMessage(), x.getMessage())
      );
      if (log.isTraceEnabled()) {
        log.trace(x.getClass().getSimpleName(), x);
      }
    }
  }

  private void tearDown(A4TcpIo target, Exception e) {
    logError(e);
    A4Io.close(target);
    if (client != null || backend != null) {
      bkSel.contextOf(backend.backend).trackConn(false);
      this.onTearDown.accept(this);
      this.client = null;
      this.backend = null;
      this.bkSel = null;
      this.tlsSni = null;
      this.tlsMatch = null;
      this.onInit = null;
      this.onTearDown = null;
      if (log.isDebugEnabled()) {
        log.debug("---------------------------------------");
      }
    }
  }

  private String id(boolean reverse) {
    if (reverse) {
      return format("%s%s", backend != null ? backend.id : "----", client.id);
    } else {
      return format("%s%s", client.id, backend != null ? backend.id : "----");
    }
  }

  public String id() {
    return id(false);
  }

  private void initBackend() {
    if (this.backend != null) {
      return;
    }
    if (tlsClient && tlsSni == null) {
      return;
    }
    this.backend = bkSel.assign(client.socket, tlsSni);
    this.bkSel.contextOf(backend.backend).trackConn(true);
    this.onInit.accept(this);
  }

  @Override public boolean matches(SNIServerName sn) {
    var sni = A4TlsCerts.sniOf(sn).orElseThrow();
    var op = bkSel.matches(client.socket, sni);
    if (op.isPresent()) {
      this.tlsSni = sni;
      this.tlsMatch = op.get();
      if (log.isTraceEnabled()) {
        log.trace("{} | {} | {}", format("%s----", client.id), Tls, this.tlsMatch);
      }
      return true;
    }
    return false;
  }

  private void logState(String op, long bt, boolean fromClient) {
    if (log.isDebugEnabled()) {
      log.debug(
        "{} | {} | {} | {} {}",
        id(!fromClient),
        op,
        format("%010d", bt),
        client == null ? "?" : client,
        backend == null ? "?" : backend
      );
    }
  }

  private void pump(boolean fromClient) {
    A4TcpIo in = null, out;
    try {
      while (true) {
        in = fromClient ? client : backend;
        out = fromClient ? backend : client;
        var op = fromClient ? Tx : Rx;
        var bt = (long) in.read();
        if (fromClient && this.backend == null) {
          initBackend();
          Thread.currentThread().setName(id());
          Thread.ofVirtual().name(id()).start(() -> pump(false));
          out = backend;
        }
        if (bt > 0) {
          in.writeTo(out);
          bkSel.contextOf(backend.backend).trackRxTx(!fromClient, bt);
          logState(op, bt, fromClient);
        }
        if (bt < 0) {
          logState(Close, bt, fromClient);
          tearDown(in, null);
          break;
        }
      }
    } catch (Exception e) {
      tearDown(in, e);
    }
  }

  public void start() {
    Thread.ofVirtual().name(id()).start(() -> pump(true));
  }

  public A4TcpSess withClient(A4TcpIo client) {
    this.client = Objects.requireNonNull(client);
    return this;
  }

  @Override public void close() {
    this.tearDown(this.client, null);
    this.tearDown(this.backend, null);
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