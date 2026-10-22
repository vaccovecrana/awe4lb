package io.vacco.a4lb.cfg;

public class A4StatsBackend {

  public String id;
  public String serverId;
  public String serverType;
  public String matchLabel;
  public String poolType;
  public String host;
  public int port;
  public String state;

  public int activeConnections;
  public long totalConnections;
  public long rxBytes;
  public long txBytes;
  public double rxBps;
  public double txBps;

}
