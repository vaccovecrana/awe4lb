package io.vacco.a4lb.tcp;

import io.vacco.a4lb.cfg.A4Server;
import io.vacco.a4lb.impl.A4Srv;
import io.vacco.a4lb.sel.A4Selector;
import io.vacco.a4lb.util.A4Io;
import org.slf4j.*;
import javax.net.ssl.*;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class A4TcpSrv implements A4Srv {

  public static final Logger log = LoggerFactory.getLogger(A4TcpSrv.class);

  private final ServerSocket serverSocket;
  private final A4Server srvConfig;
  private final A4Selector bkSel;
  private final Map<String, A4TcpSess> sessions = new ConcurrentHashMap<>();

  public A4TcpSrv(A4Server srv, A4Selector bkSel) {
    try {
      this.srvConfig = Objects.requireNonNull(srv);
      this.bkSel = Objects.requireNonNull(bkSel);
      var addr = new InetSocketAddress(srv.addr.host, srv.addr.port);
      if (srvConfig.tls != null) {
        log.info("{} - initializing SSL context", srv.id);
        var factory = A4TlsCerts.forServer(srv).getServerSocketFactory();
        var sslSocket = (SSLServerSocket) factory.createServerSocket();
        sslSocket.setReuseAddress(true);
        sslSocket.bind(addr);
        sslSocket.setEnabledProtocols(srvConfig.tls.protocols != null ? srvConfig.tls.protocols : sslSocket.getSupportedProtocols());
        sslSocket.setEnabledCipherSuites(srvConfig.tls.ciphers != null ? srvConfig.tls.ciphers : sslSocket.getSupportedCipherSuites());
        sslSocket.setNeedClientAuth(false);
        this.serverSocket = sslSocket;
      } else {
        var plainSocket = new ServerSocket();
        plainSocket.setReuseAddress(true);
        plainSocket.bind(addr);
        this.serverSocket = plainSocket;
      }
      log.info("{} - {} - TCP ingress open", srv.id, serverSocket.getLocalSocketAddress());
    } catch (IOException ioe) {
      log.error("Unable to open server socket {}", srv.addr, ioe);
      throw new IllegalStateException(ioe);
    }
  }

  private A4TcpSess initSession() {
    Socket clientSocket = null;
    try {
      // TODO check for connection limits here.
      var isTls = srvConfig.tls != null;
      var sess = new A4TcpSess(
        this.bkSel,
        s0 -> sessions.put(s0.id(), s0),
        s0 -> sessions.remove(s0.id()),
        isTls
      );
      clientSocket = serverSocket.accept();
      if (isTls && clientSocket instanceof SSLSocket sslSocket) {
        var params = sslSocket.getSSLParameters();
        params.setSNIMatchers(Collections.singletonList(sess));
        sslSocket.setSSLParameters(params);
      }
      return sess.withClient(new A4TcpIo(clientSocket));
    } catch (Exception ioe) {
      log.error("{} - Unable to initialize tcp session", serverSocket.getLocalSocketAddress(), ioe);
      if (clientSocket != null) {
        A4Io.close(clientSocket);
      }
      return null;
    }
  }

  @Override public Void call() {
    while (true) {
      var sess = initSession();
      if (sess != null) {
        sess.start();
      }
    }
  }

  @Override public void close() {
    A4Io.close(serverSocket);
    int sessCount = this.sessions.size();
    this.sessions.values().forEach(A4Io::close);
    this.sessions.clear();
    log.info(
      "{} - {} - TCP ingress closed{}",
      srvConfig.id, serverSocket.getLocalSocketAddress(),
      sessCount > 0 ? " (" + sessCount + ") sessions" : ""
    );
  }

}