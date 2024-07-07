package io.vacco.a4lb.tcp;

import io.vacco.a4lb.niossl.*;
import io.vacco.a4lb.sel.A4Selector;
import io.vacco.a4lb.util.A4Io;
import org.slf4j.*;
import javax.net.ssl.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;

import static io.vacco.a4lb.util.A4Logging.*;
import static io.vacco.a4lb.util.A4Io.*;
import static java.lang.String.format;

public class A4TcpSess extends SNIMatcher {

  private static final Logger log = LoggerFactory.getLogger(A4TcpSess.class);

  public  A4TcpSrv owner;
  private final A4Selector bkSel;

  private A4TcpIo client, backend;
  private String id;

  private String tlsSni;
  private final ExecutorService tlsExec;
  private final boolean tlsClient;

  public A4TcpSess(A4TcpSrv owner, A4Selector bkSel, boolean tlsClient, ExecutorService tlsExec) {
    super(0);
    this.owner = Objects.requireNonNull(owner);
    this.bkSel = Objects.requireNonNull(bkSel);
    this.tlsClient = tlsClient;
    this.tlsExec = tlsExec;
  }

  private void logState(Integer bytes, A4TcpIo io) {
    if (log.isDebugEnabled()) {
      log.debug("{} - {}, {}", id, bytes, io);
    }
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
    this.owner = null;
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
  }

  @Override public boolean matches(SNIServerName sn) {
    var sni = SSLCertificates.sniOf(sn).orElseThrow();
    var op = bkSel.matches(client.channel, sni);
    if (op.isPresent()) {
      this.tlsSni = sni;
      return true;
    }
    return false;
  }

  /*
    TODO
      bkSel.contextOf(backend.backend).trackRxTx(false, bytes);
      bkSel.contextOf(backend.backend).trackRxTx(true, bytes);
   */

  private void tcpUpdate(SelectionKey key) {
    var isCl = client.channelKey == key;
    var isBk = backend != null && backend.channelKey == key;
    var isClRd = key.isReadable() && isCl;
    var isClWr = key.isWritable() && isCl;
    var isBkRd = key.isReadable() && isBk;
    var isBkWr = key.isWritable() && isBk;
    var bytes = (Integer) null;

    if (isClRd) {
      bytes = client.read();
    } else if (isClWr) {
      bytes = backend.writeTo(client.channel);
    } else if (isBkRd) {
      bytes = backend.read();
    } else if (isBkWr) {
      bytes = client.writeTo(backend.channel);
    }

    logState(bytes, isCl ? client : backend);

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
        return;
      }
      sessionMismatch(key);
    } catch (Exception e) {
      tearDown(e);
    }
  }

  public A4TcpSess withClient(A4TcpIo client) {
    this.client = Objects.requireNonNull(client);
    this.client.channelKey.attach(this);
    return this;
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
