package io.vacco.a4lb.cfg;

import java.util.ArrayList;
import java.util.List;

public class A4Config {

  public static final long Seed = 1984;

  public String id, description;
  public A4Sock api;
  public List<A4Server> servers = new ArrayList<>();

  public A4Config server(A4Server server) {
    servers.add(server);
    return this;
  }

  public List<A4Server> serverList() {
    return servers;
  }

  /*
   * < You are the Director now >
   * < We expect Independence/Dependence >
   * < You are Authority/Chosen One >
   * < The Bureau/Game needs you >
   */

}
