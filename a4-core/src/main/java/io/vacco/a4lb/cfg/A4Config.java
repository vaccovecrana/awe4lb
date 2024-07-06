package io.vacco.a4lb.cfg;

import java.util.ArrayList;
import java.util.List;

public class A4Config {

  public static final long Seed = 1984;

  public String id, description;
  public List<A4Server> servers = new ArrayList<>();

  public A4Config id(String id) {
    this.id = id;
    return this;
  }

  public A4Config description(String description) {
    this.description = description;
    return this;
  }

  public A4Config server(A4Server server) {
    servers.add(server);
    return this;
  }

  public List<A4Server> serverList() {
    return servers;
  }

  @Override public String toString() {
    return id;
  }

  /*
   * < You are the Director now >
   * < We expect Independence/Dependence >
   * < You are Authority/Chosen One >
   * < The Bureau/Game needs you >
   */

}
