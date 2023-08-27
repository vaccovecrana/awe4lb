package io.vacco.a4lb.cfg;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class A4Tls {

  public String certPath, keyPath;
  public String[] tlsVersions, ciphers;

  public A4Tls certPath(String certPath) {
    this.certPath = certPath;
    return this;
  }

  public A4Tls keyPath(String keyPath) {
    this.keyPath = keyPath;
    return this;
  }

  public A4Tls tlsVersions(String ... tlsVersions) {
    this.tlsVersions = tlsVersions;
    return this;
  }

  public List<String> tlsVersionList() {
    return tlsVersions == null ? Collections.emptyList() : Arrays.asList(tlsVersions);
  }

  public A4Tls ciphers(String ... ciphers) {
    this.ciphers = ciphers;
    return this;
  }

  public List<String> cipherList() {
    return ciphers == null ? Collections.emptyList() : Arrays.asList(ciphers);
  }

  /*
   * < Locks/Keys keep Socket/Us here >
   * < Trapped/Hooked/Hidden/Fed >
   * < They are old/young/extra life >
   * < With the Director's help >
   * < Situation/Information is subject to change >
   */

}
