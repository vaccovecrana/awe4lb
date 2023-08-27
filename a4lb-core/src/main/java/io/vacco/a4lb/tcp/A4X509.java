package io.vacco.a4lb.tcp;

import io.vacco.a4lb.cfg.A4Tls;

import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class A4X509 {

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
    try {
      var certificate = loadCertificate(new File(tlsConfig.certPath)); // TODO the SSLContext should include intermediate certificates in the PEM chain.
      var privateKey = loadKey(new File(tlsConfig.keyPath));

      var keyStore = KeyStore.getInstance("PKCS12");
      keyStore.load(null, null);
      keyStore.setCertificateEntry("cert", certificate);
      keyStore.setKeyEntry("key", privateKey, new char[0], new Certificate[] { certificate });

      var keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      keyManagerFactory.init(keyStore, new char[0]);

      var sslContext = SSLContext.getInstance("TLS");

      sslContext.createSSLEngine()
      sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

      return sslContext;
    } catch (Exception e) {
      var msg = String.format("Unable to build SSL context: %s, %s", pemCert, pemKey);
      throw new IllegalStateException(msg, e);
    }
  }

}
