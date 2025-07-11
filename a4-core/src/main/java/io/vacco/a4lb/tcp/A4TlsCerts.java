package io.vacco.a4lb.tcp;

import io.vacco.a4lb.cfg.A4Server;
import javax.net.ssl.*;
import java.security.*;
import java.security.cert.*;
import java.util.*;

public class A4TlsCerts {

  public static SSLContext trustAllContext() {
    try {
      var sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, new TrustManager[] {
          new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() { return null; }
            public void checkClientTrusted(X509Certificate[] certs, String authType) {}
            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
          }
      }, new SecureRandom());
      return sslContext;
    } catch (Exception e) {
      throw new IllegalStateException("Unable to initialize all-trust SSL context", e);
    }
  }

  public static SSLContext forServer(A4Server srv) {
    try {
      var km = new A4TlsKeyManager();
      var sslContext = SSLContext.getInstance("TLS");
      for (var match : srv.match) {
        if (match.tls != null) {
          km.add(match.tls, match.matchLabel());
        }
      }
      if (srv.tls != null && srv.tls.base != null) {
        km.add(srv.tls.base, A4TlsKeyManager.Base);
      }
      sslContext.init(new KeyManager[] { km }, null, null);
      return sslContext;
    } catch (Exception e) {
      throw new IllegalStateException("Unable to initialize SSLContext", e);
    }
  }

  public static Optional<String> sniOf(SNIServerName sni) {
    if (!(sni instanceof SNIHostName)) {
      return Optional.empty();
    }
    return Optional.of(((SNIHostName) sni).getAsciiName());
  }

}
