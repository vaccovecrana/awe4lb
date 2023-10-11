package io.vacco.a4lb.api;

import io.vacco.a4lb.cfg.A4Config;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

public class A4Controller {

  public static final String apiV1Config = "/api/v1/config";

  @GET @Path(apiV1Config)
  public A4Config getActiveConfig() {
    return new A4Config();
  }

}
