package io.vacco.a4lb.web;

import com.google.gson.Gson;
import io.vacco.a4lb.cfg.A4ConfigState;
import io.vacco.a4lb.service.A4Service;
import io.vacco.a4lb.cfg.*;
import io.vacco.ronove.RvResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.io.File;
import java.util.*;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.vacco.a4lb.service.A4Service.New;
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
  public RvResponse<A4Config> apiV1ConfigGet(@QueryParam(pConfigId) String configId) {
    var res = new RvResponse<A4Config>().withStatus(Response.Status.OK);
    try {
      if (configId != null) {
        if (configId.equals(New)) {
          return res.withBody(new A4Config());
        }
        var cfg = loadFromOrFail(configFileOf(configRoot, configId), gson);
        return res.withBody(deflate(cfg, gson));
      }
      var cfg = service.instance != null ? service.instance.config : new A4Config();
      return res.withBody(cfg);
    } catch (Exception e) {
      return res.withStatus(Response.Status.NOT_FOUND);
    }
  }

  @POST @Path(apiV1Config)
  public RvResponse<Collection<A4Validation>> apiV1ConfigPost(@QueryParam(pConfigId) String configId,
                                                              @BeanParam A4Config config) {
    var errors = service.update(configRoot, configId, inflate(config));
    return new RvResponse<Collection<A4Validation>>()
        .withStatus(errors.isEmpty() ? Response.Status.OK : Response.Status.CONFLICT)
        .withBody(errors);
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
      syncFs(configRoot, gson, state.active);
    }
    if (state.inactive != null) {
      syncFs(configRoot, gson, state.inactive);
    }
    return state;
  }

  @GET @Path(apiV1ConfigList)
  public Collection<A4Config> apiV1ConfigListGet() {
    return configList(configRoot, gson).collect(Collectors.toList());
  }

}
