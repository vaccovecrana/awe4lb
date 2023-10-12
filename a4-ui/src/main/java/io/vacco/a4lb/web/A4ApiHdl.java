package io.vacco.a4lb.web;

import io.vacco.a4lb.A4Service;
import io.vacco.a4lb.cfg.A4Config;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.util.Objects;

import static io.vacco.a4lb.web.A4Route.*;

public class A4ApiHdl {

  public A4Service service;

  public A4ApiHdl(A4Service service) {
    this.service = Objects.requireNonNull(service);
  }

  @GET @Path(apiV1Config)
  public A4Config getActiveConfig() {
    return service.config != null
        ? service.config
        : new A4Config();
  }

}
