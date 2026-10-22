package io.vacco.a4lb.tcp;

import io.vacco.a4lb.cfg.A4Server;
import io.vacco.a4lb.cfg.A4StatsServer;
import io.vacco.a4lb.cfg.A4Tcp;
import io.vacco.a4lb.impl.A4Srv;
import io.vacco.a4lb.sel.A4Selector;
import io.vacco.a4lb.util.A4Io;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLServerSocket;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;

import static io.vacco.a4lb.util.A4Logging.onError;

public class A4TcpSrv implements A4Srv {

  public static final Logger log = LoggerFactory.getLogger(A4TcpSrv.class);
  private static final int Backlog = 4096;

  private final ServerSocket serverSocket;
  private final A4Server srvConfig;
  private final A4Selector bkSel;
  private final A4TcpSessions sessions;

  public A4TcpSrv(A4Server srv, A4Selector bkSel) {
    try {
      this.srvConfig = Objects.requireNonNull(srv);
      this.bkSel = Objects.requireNonNull(bkSel);
      this.sessions = new A4TcpSessions(srv.id, srv.tcp != null ? srv.tcp : new A4Tcp());
      var addr = new InetSocketAddress(srv.addr.host, srv.addr.port);
      if (srvConfig.tls != null) {
        log.info("{} - initializing SSL context", srv.id);
        var factory = A4TlsCerts.forServer(srv).getServerSocketFactory();
        var sslSocket = (SSLServerSocket) factory.createServerSocket();
        sslSocket.setReuseAddress(true);
        sslSocket.bind(addr, Backlog);
        sslSocket.setEnabledProtocols(srvConfig.tls.protocols != null ? srvConfig.tls.protocols : sslSocket.getSupportedProtocols());
        sslSocket.setEnabledCipherSuites(srvConfig.tls.ciphers != null ? srvConfig.tls.ciphers : sslSocket.getSupportedCipherSuites());
        sslSocket.setNeedClientAuth(false);
        this.serverSocket = sslSocket;
      } else {
        var plainSocket = new ServerSocket();
        plainSocket.setReuseAddress(true);
        plainSocket.bind(addr, Backlog);
        this.serverSocket = plainSocket;
      }
      log.info("{} - {} - TCP ingress open", srv.id, serverSocket.getLocalSocketAddress());
    } catch (IOException ioe) {
      onError(log, "Unable to open server socket {}", ioe, srv.addr);
      throw new IllegalStateException(ioe);
    }
  }

  @Override
  public Void call() {
    while (!serverSocket.isClosed()) {
      Socket clientSocket;
      try {
        clientSocket = serverSocket.accept();
      } catch (Exception e) {
        if (serverSocket.isClosed()) {
          break;
        }
        onError(log, "{} - Unable to accept tcp connection", e, serverSocket.getLocalSocketAddress());
        try {
          // Back off briefly on transient accept() failures; avoid a tight spin.
          Thread.sleep(50);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          break;
        }
        continue;
      }
      var sess = sessions.open(bkSel, clientSocket, srvConfig.tls != null);
      if (sess != null) {
        sess.start();
      }
    }
    return null;
  }

  @Override
  public void close() {
    A4Io.close(serverSocket);
    int sessCount = sessions.closeAll();
    log.info(
      "{} - {} - TCP ingress closed{}",
      srvConfig.id, serverSocket.getLocalSocketAddress(),
      sessCount > 0 ? " (" + sessCount + " sessions)" : ""
    );
  }

  public A4StatsServer stats() {
    var stats = sessions.stats();
    stats.serverType = srvConfig.tls != null ? "TLS" : "TCP";
    return stats;
  }

}