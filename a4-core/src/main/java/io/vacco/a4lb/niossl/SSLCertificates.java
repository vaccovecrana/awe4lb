package io.vacco.a4lb.niossl;

import io.vacco.a4lb.cfg.A4Tls;
import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

public class SSLCertificates {

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

  private static X509Certificate parseCertificate(String pem) {
    try {
      var certificateFactory = CertificateFactory.getInstance("X.509");
      var certBytes = Base64.getMimeDecoder().decode(pem);
      return (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(certBytes));
    } catch (Exception e) {
      throw new IllegalStateException("Unable to parse certificate: " + e.getMessage(), e);
    }
  }

  public static List<X509Certificate> loadCertificates(File pemCert) {
    var certificates = new ArrayList<X509Certificate>();
    try (var reader = new BufferedReader(new InputStreamReader(new FileInputStream(pemCert)))) {
      var line = "";
      var certStringBuilder = new StringBuilder();
      var inCertBlock = false;

      while ((line = reader.readLine()) != null) {
        if (line.startsWith("-----BEGIN CERTIFICATE-----")) {
          inCertBlock = true;
          certStringBuilder = new StringBuilder();
        } else if (line.startsWith("-----END CERTIFICATE-----")) {
          inCertBlock = false;
          var certPem = certStringBuilder.toString();
          var certificate = parseCertificate(certPem);
          certificates.add(certificate);
        } else if (inCertBlock) {
          certStringBuilder.append(line).append('\n');
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Unable to load certificate: " + pemCert.getAbsolutePath(), e);
    }
    return certificates;
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
      var certificates = loadCertificates(pemCert);
      var privateKey = loadKey(pemKey);

      var keyStore = KeyStore.getInstance("PKCS12");
      keyStore.load(null, null);
      keyStore.setKeyEntry("key", privateKey, new char[0], certificates.toArray(new Certificate[0]));

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

  public static Optional<String> sniOf(SNIServerName sni) {
    if (!(sni instanceof SNIHostName)) {
      return Optional.empty();
    }
    return Optional.of(((SNIHostName) sni).getAsciiName());
  }

}
