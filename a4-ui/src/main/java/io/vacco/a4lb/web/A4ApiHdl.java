package io.vacco.a4lb.web;

import io.vacco.a4lb.A4Service;
import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.util.A4Valid;
import jakarta.ws.rs.*;
import java.util.*;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.vacco.a4lb.web.A4Route.*;

public class A4ApiHdl {

  public A4Service service;

  public A4ApiHdl(A4Service service) {
    this.service = Objects.requireNonNull(service);
  }

  @GET @Path(apiV1Config)
  public A4Config apiV1ConfigGet() {
    return service.instance.config;
  }

  @POST @Path(apiV1Config)
  public Collection<A4Validation> apiV1ConfigPost(@BeanParam A4Config config) {
    var errList = service.add(config);
    return errList.isEmpty() ? Collections.emptyList() : A4Valid.validationsOf(errList);
  }

  @DELETE @Path(apiV1Config)
  public boolean apiV1ConfigDelete(@QueryParam("configId") String configId) {
    return service.delete(configId);
  }

  @GET @Path(apiV1ConfigList)
  public Collection<A4Config> apiV1ConfigListGet() {
    return service.rootConfigs().collect(Collectors.toList());
  }

}
