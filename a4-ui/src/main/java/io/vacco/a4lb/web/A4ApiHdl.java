package io.vacco.a4lb.web;

import com.google.gson.Gson;
import io.vacco.a4lb.cfg.A4ConfigState;
import io.vacco.a4lb.service.A4Service;
import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.util.A4Valid;
import jakarta.ws.rs.*;

import java.io.File;
import java.util.*;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.vacco.a4lb.util.A4Configs.*;
import static io.vacco.a4lb.web.A4Route.*;

public class A4ApiHdl {

  private final File      configRoot;
  private final A4Service service;
  private final Gson      gson;

  public A4ApiHdl(File configRoot, A4Service service, Gson gson) {
    this.configRoot = Objects.requireNonNull(configRoot);
    this.service = Objects.requireNonNull(service);
    this.gson = Objects.requireNonNull(gson);
  }

  @GET @Path(apiV1Config)
  public A4Config apiV1ConfigGet() {
    return service.instance != null ? service.instance.config : new A4Config();
  }

  @POST @Path(apiV1Config)
  public Collection<A4Validation> apiV1ConfigPost(@BeanParam A4Config config) {
    var errList = save(configRoot, gson, config);
    return errList.isEmpty() ? Collections.emptyList() : A4Valid.validationsOf(errList);
  }

  @DELETE @Path(apiV1Config)
  public boolean apiV1ConfigDelete(@QueryParam(pConfigId) String configId) {
    return configId != null && delete(configRoot, configId);
  }

  @GET @Path(apiV1ConfigSelect)
  public A4ConfigState apiV1ConfigSelectGet(@QueryParam(pConfigId) String configId) {
    var state = configId == null || configId.isEmpty()
        ? service.setActive(null)
        : service.setActive(loadFromOrFail(configFileOf(configRoot, configId), gson));
    if (state.active != null) {
      syncFs(configRoot, gson, state.active, true);
    }
    if (state.inactive != null) {
      syncFs(configRoot, gson, state.inactive, false);
    }
    return state;
  }

  @GET @Path(apiV1ConfigList)
  public Collection<A4Config> apiV1ConfigListGet() {
    return configList(configRoot, gson).collect(Collectors.toList());
  }

}
