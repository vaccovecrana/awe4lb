package io.vacco.a4lb.cfg;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class A4Tls {

  public String certPath, keyPath;
  public String[] protocols, ciphers;

  public A4Tls certPath(String certPath) {
    this.certPath = certPath;
    return this;
  }

  public A4Tls keyPath(String keyPath) {
    this.keyPath = keyPath;
    return this;
  }

  public A4Tls protocols(String ... versions) {
    this.protocols = versions;
    return this;
  }

  public List<String> protocolList() {
    return protocols == null ? Collections.emptyList() : Arrays.asList(protocols);
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
