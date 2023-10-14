package io.vacco.a4lb.web;

import io.vacco.a4lb.A4Service;
import io.vacco.a4lb.cfg.A4Config;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.vacco.a4lb.web.A4Route.*;

public class A4ApiHdl {

  public A4Service service;

  public A4ApiHdl(A4Service service) {
    this.service = Objects.requireNonNull(service);
  }

  @GET @Path(apiV1Config)
  public Collection<A4Config> getInstances() {
    return service.instances.values().stream().map(lb -> lb.config).collect(Collectors.toList());
  }

}
