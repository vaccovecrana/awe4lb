package io.vacco.a4lb.cfg;

public class A4StatsServer {

  public String id;

  public int activeConnections;
  public int activeTlsHandshakes;

  public long rejectedConnections;
  public long rejectedTlsHandshakes;
  public long tlsHandshakeTimeouts;

}
