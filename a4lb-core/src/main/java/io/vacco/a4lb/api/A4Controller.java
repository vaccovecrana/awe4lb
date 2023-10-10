package io.vacco.a4lb.api;

import io.vacco.a4lb.cfg.A4Config;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

public class A4Controller {

  @GET @Path("/v1/config")
  public A4Config getActiveConfig() {
    return new A4Config();
  }

}
