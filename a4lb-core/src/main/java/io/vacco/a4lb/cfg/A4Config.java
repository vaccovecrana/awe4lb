package io.vacco.a4lb.cfg;

import java.util.LinkedHashMap;
import java.util.Map;

public class A4Config {

  public A4Probe api, metrics;
  public Map<String, A4Server> servers = new LinkedHashMap<>();

  public Map<String, A4Server> serverIdx() {
    return servers;
  }

  public A4Config server(String id, A4Server server) {
    servers.put(id, server);
    return this;
  }

}
