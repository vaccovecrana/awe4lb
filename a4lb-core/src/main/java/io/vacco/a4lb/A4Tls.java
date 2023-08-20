package io.vacco.a4lb;

import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class A4Tls {

  public static SSLContext contextFrom(File pemCert, File pemKey) {
    try {
      var certificateFactory = CertificateFactory.getInstance("X.509");
      var certInputStream = new FileInputStream(pemCert);
      var certificate = (X509Certificate) certificateFactory.generateCertificate(certInputStream);

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
      var privateKey = keyFactory.generatePrivate(keySpec);

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

}
