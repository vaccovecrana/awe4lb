package io.vacco.a4lb.cfg;

public class A4Tcp {

  public static final int DefaultMaxConnectionsPerIp = 512;
  public static final int DefaultTlsHandshakeTimeoutMs = 5000;
  public static final int DefaultMaxTlsHandshakes = 256;

  public int maxConnectionsPerIp = DefaultMaxConnectionsPerIp;
  public int tlsHandshakeTimeoutMs = DefaultTlsHandshakeTimeoutMs;
  public int maxTlsHandshakes = DefaultMaxTlsHandshakes;

}
