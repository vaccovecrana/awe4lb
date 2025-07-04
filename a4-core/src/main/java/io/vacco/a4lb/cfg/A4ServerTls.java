package io.vacco.a4lb.cfg;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class A4ServerTls {

  public String[] protocols, ciphers;
  public A4Tls    base;

  public A4ServerTls protocols(String ... versions) {
    this.protocols = versions;
    return this;
  }

  public List<String> protocolList() {
    return protocols == null ? Collections.emptyList() : Arrays.asList(protocols);
  }

  public A4ServerTls ciphers(String ... ciphers) {
    this.ciphers = ciphers;
    return this;
  }

  public List<String> cipherList() {
    return ciphers == null ? Collections.emptyList() : Arrays.asList(ciphers);
  }

}
