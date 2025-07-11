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

  public static final String Close = "✖ ", Rx = "↓ ", Tx = "↑ ", Tls = "\uD83D\uDD12";

  private static final Logger log = LoggerFactory.getLogger(A4TcpSess.class);

  public String id;

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
        "{} ❌ {} - {} - {}",
        Thread.currentThread().getName(),
        e.getClass().getSimpleName(),
        x.getClass().getSimpleName(),
        e == x ? e.getMessage() : format("%s - %s", e.getMessage(), x.getMessage())
      );
      if (log.isTraceEnabled()) {
        log.trace(x.getMessage(), x);
      }
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
    this.tlsSni = null;
    this.tlsMatch = null;
    this.onInit = null;
    this.onTearDown = null;
    if (log.isDebugEnabled()) {
      log.debug("------------------------------");
    }
  }

  private String threadIdOf(A4TcpIo io0, A4TcpIo io1) {
    return format("%s%s", io0.id, io1.id);
  }

  private void initBackend() {
    if (this.backend != null) {
      return;
    }
    if (tlsClient && tlsSni == null) {
      return;
    }
    this.backend = bkSel.assign(client.socket, tlsSni);
    this.id =  threadIdOf(client, backend);
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
        log.trace("{} - SNI match found: {}", format("%s----", client.id), this.tlsMatch);
      }
      return true;
    }
    return false;
  }

  private void logState(String op, long bt) {
    if (log.isDebugEnabled()) {
      log.debug(
        "{} | {} | t{} | c{} b{}",
        Thread.currentThread().getName(),
        op,
        format("%016d", bt),
        client == null ? "?" : client,
        backend == null ? "?" : backend
      );
    }
  }

  private void pump(boolean fromClient) {
    try {
      while (true) {
        var in = fromClient ? client : backend;
        var out = fromClient ? backend : client;
        var op = fromClient ? Tx : Rx;
        if (fromClient && this.backend == null) {
          var temp = new byte[512]; // Small buffer to trigger TLS handshake
          int br = in.socket.getInputStream().read(temp);
          if (br < 0) {
            logState(Close, br);
            break;
          } else {
            initBackend();
            Thread.currentThread().setName(this.id);
            Thread.ofVirtual().name(id).start(() -> pump(false));
            backend.socket.getOutputStream().write(temp, 0, br);
            backend.socket.getOutputStream().flush();
            bkSel.contextOf(backend.backend).trackRxTx(false, br);
            logState(op, br);
            continue;
          }
        }
        var bytes = in.transferTo(out);
        out.socket.shutdownOutput();
        bkSel.contextOf(backend.backend).trackRxTx(!fromClient, bytes);
        logState(op, bytes);
        break;
      }
      if (fromClient) {
        tearDown(null);
      }
    } catch (Exception e) {
      if (fromClient) {
        tearDown(e);
      } else {
        logError(e);
      }
    }
  }

  public void start() {
    Thread.ofVirtual().start(() -> pump(true));
  }

  public A4TcpSess withClient(A4TcpIo client) {
    this.client = Objects.requireNonNull(client);
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