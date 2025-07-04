package io.vacco.a4lb.tcp;

import io.vacco.a4lb.cfg.A4Tls;
import javax.net.ssl.*;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

public class A4TlsKeyManager extends X509ExtendedKeyManager {

  public static final String Base = "a4lb.base";

  private final Map<String, KeyStore.PrivateKeyEntry> matchKeys = new HashMap<>();
  private final X509ExtendedKeyManager defaultKeyManager;

  public A4TlsKeyManager() {
    try {
      var keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      keyManagerFactory.init(null, null);
      this.defaultKeyManager = (X509ExtendedKeyManager) keyManagerFactory.getKeyManagers()[0];
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private X509Certificate parseCertificate(String pem) {
    try {
      var certificateFactory = CertificateFactory.getInstance("X.509");
      var certBytes = Base64.getMimeDecoder().decode(pem);
      return (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(certBytes));
    } catch (Exception e) {
      throw new IllegalStateException("Unable to parse certificate: " + e.getMessage(), e);
    }
  }

  public List<X509Certificate> loadCertificates(File pemCert) {
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

  private PrivateKey tryLoadKey(PKCS8EncodedKeySpec keySpec, String filePath) {
    try {
      return KeyFactory.getInstance("EC").generatePrivate(keySpec);
    } catch (Exception e) {
      try {
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
      } catch (Exception e2) {
        try {
          return KeyFactory.getInstance("Ed25519").generatePrivate(keySpec);
        } catch (Exception e3) {
          try {
            return KeyFactory.getInstance("DSA").generatePrivate(keySpec);
          } catch (Exception e4) {
            throw new IllegalStateException("Unsupported key type in: " + filePath, e4);
          }
        }
      }
    }
  }

  public PrivateKey loadKey(File pemKey) {
    try {
      byte[] keyBytes;
      try (FileInputStream keyInputStream = new FileInputStream(pemKey)) {
        keyBytes = keyInputStream.readAllBytes();
      }
      var keyPEM = new String(keyBytes)
        .replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replaceAll("\\s+", "");
      var pkcs8Key = Base64.getDecoder().decode(keyPEM);
      var keySpec = new PKCS8EncodedKeySpec(pkcs8Key);
      return tryLoadKey(keySpec, pemKey.getAbsolutePath());
    } catch (Exception e) {
      throw new IllegalStateException("Unable to load private key: " + pemKey.getAbsolutePath(), e);
    }
  }

  public void add(A4Tls tls, String alias) {
    var pemCert = new File(tls.certPath);
    var pemKey = new File(tls.keyPath);
    try {
      var certificates = loadCertificates(pemCert);
      var privateKey = loadKey(pemKey);
      var entry = new KeyStore.PrivateKeyEntry(privateKey, certificates.toArray(new Certificate[0]));
      this.matchKeys.put(alias, entry);
    } catch (Exception e) {
      var msg = String.format("Unable to load certificates for %s", alias);
      throw new IllegalStateException(msg, e);
    }
  }

  @Override public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
    if (engine != null) {
      var params = engine.getSSLParameters();
      for (var matcher : params.getSNIMatchers()) {
        if (matcher instanceof A4TcpSess) {
          var sess = (A4TcpSess) matcher;
          var match = sess.getTlsMatch();
          if (match.tls != null) {
            return match.matchLabel();
          }
        }
      }
      if (matchKeys.containsKey(Base)) {
        return Base;
      }
    }
    return defaultKeyManager.chooseEngineServerAlias(keyType, issuers, null);
  }

  @Override public PrivateKey getPrivateKey(String alias) {
    if (matchKeys.containsKey(alias)) {
      return matchKeys.get(alias).getPrivateKey();
    }
    return defaultKeyManager.getPrivateKey(alias);
  }

  @Override public String[] getClientAliases(String keyType, Principal[] issuers) {
    return defaultKeyManager.getClientAliases(keyType, issuers);
  }

  @Override public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
    return defaultKeyManager.chooseClientAlias(keyType, issuers, socket);
  }

  @Override public String[] getServerAliases(String keyType, Principal[] issuers) {
    return defaultKeyManager.getServerAliases(keyType, issuers);
  }

  @Override public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
    return defaultKeyManager.chooseServerAlias(keyType, issuers, socket);
  }

  @Override public X509Certificate[] getCertificateChain(String alias) {
    if (matchKeys.containsKey(alias)) {
      return (X509Certificate[]) matchKeys.get(alias).getCertificateChain();
    }
    return defaultKeyManager.getCertificateChain(alias);
  }

}
