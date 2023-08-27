package io.vacco.a4lb.tcp;

import io.vacco.a4lb.cfg.A4Tls;
import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Optional;

public class A4Ssl {

  public static X509Certificate loadCertificate(File pemCert) {
    try {
      var certificateFactory = CertificateFactory.getInstance("X.509");
      var certInputStream = new FileInputStream(pemCert);
      return (X509Certificate) certificateFactory.generateCertificate(certInputStream);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to load certificate: " + pemCert.getAbsolutePath(), e);
    }
  }

  public static PrivateKey loadKey(File pemKey) {
    try {
      var keyInputStream = new FileInputStream(pemKey);
      var keyBytes = new byte[keyInputStream.available()];
      keyInputStream.read(keyBytes);
      keyInputStream.close();

      var keyPEM = new String(keyBytes);
      var privateKeyPEM = keyPEM.replace("-----BEGIN PRIVATE KEY-----", "")
          .replace("-----END PRIVATE KEY-----", "")
          .replace("\n", "");

      var encodedKey = Base64.getDecoder().decode(privateKeyPEM);
      var keySpec = new PKCS8EncodedKeySpec(encodedKey);
      var keyFactory = KeyFactory.getInstance("RSA");
      return keyFactory.generatePrivate(keySpec);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to load private key: " + pemKey.getAbsolutePath(), e);
    }
  }

  public static SSLContext contextFrom(A4Tls tlsConfig) {
    var pemCert = new File(tlsConfig.certPath);
    var pemKey = new File(tlsConfig.keyPath);
    try {
      var certificate = loadCertificate(pemCert); // TODO the SSLContext should include intermediate certificates in the PEM chain.
      var privateKey = loadKey(pemKey);

      var keyStore = KeyStore.getInstance("PKCS12");
      keyStore.load(null, null);
      keyStore.setCertificateEntry("cert", certificate);
      keyStore.setKeyEntry("key", privateKey, new char[0], new Certificate[] { certificate });

      var keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      keyManagerFactory.init(keyStore, new char[0]);

      var sslContext = SSLContext.getInstance("TLS");
      sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

      return sslContext;
    } catch (Exception e) {
      var msg = String.format("Unable to build SSL context: %s, %s", pemCert, pemKey);
      throw new IllegalStateException(msg, e);
    }
  }

  public static SSLEngine configureServer(SSLContext ctx, A4Tls tlsConfig) {
    var eng = ctx.createSSLEngine();
    eng.setUseClientMode(false);
    if (tlsConfig != null) {
      if (tlsConfig.tlsVersions != null && tlsConfig.tlsVersions.length > 0) {
        eng.setEnabledProtocols(tlsConfig.tlsVersions);
      }
      if (tlsConfig.ciphers != null && tlsConfig.ciphers.length > 0) {
        eng.setEnabledCipherSuites(tlsConfig.ciphers);
      }
    }
    return eng;
  }

  public static Optional<String> sniOf(Optional<SNIServerName> sni) {
    if (sni.isEmpty() || !(sni.get() instanceof SNIHostName)) {
      return Optional.empty();
    }
    return Optional.of(((SNIHostName) sni.get()).getAsciiName());
  }

}
