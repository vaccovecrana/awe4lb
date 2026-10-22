package io.vacco.a4lb.web;

import com.google.gson.Gson;
import io.vacco.a4lb.cfg.A4Config;
import io.vacco.a4lb.cfg.A4ConfigState;
import io.vacco.a4lb.cfg.A4Stats;
import io.vacco.a4lb.cfg.A4Validation;
import io.vacco.a4lb.service.A4Service;
import io.vacco.ronove.api.RvGraal;
import io.vacco.ronove.util.RvResponse;
import io.vacco.shax.logging.ShLogConfig;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.vacco.a4lb.service.A4Service.New;
import static io.vacco.a4lb.util.A4Configs.*;
import static io.vacco.a4lb.web.A4Route.*;

@RvGraal(include = {ShLogConfig.class})
public class A4ApiHdl {

  private final File configRoot;
  private final A4Service service;
  private final Gson gson;

  public A4ApiHdl(File configRoot, A4Service service, Gson gson) {
    this.configRoot = Objects.requireNonNull(configRoot);
    this.service = Objects.requireNonNull(service);
    this.gson = Objects.requireNonNull(gson);
  }

  @GET
  @Path(apiV1Config)
  @Produces(MediaType.APPLICATION_JSON)
  public RvResponse<A4Config> apiV1ConfigGet(@QueryParam(pConfigId) String configId) {
    var res = new RvResponse<A4Config>().withStatus(Response.Status.OK);
    try {
      if (configId != null) {
        if (configId.equals(New)) {
          return res.withBody(new A4Config());
        }
        if (!isValidId(configId)) {
          return res.withStatus(Response.Status.NOT_FOUND);
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

  @POST
  @Path(apiV1Config)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public RvResponse<Collection<A4Validation>> apiV1ConfigPost(@QueryParam(pConfigId) String configId,
                                                              @BeanParam A4Config config) {
    if (config == null) {
      return new RvResponse<Collection<A4Validation>>()
        .withStatus(Response.Status.CONFLICT)
        .withBody(List.of(A4Validation.ofMessage("Configuration is required")));
    }
    var errors = service.update(configRoot, configId, inflate(config));
    return new RvResponse<Collection<A4Validation>>()
      .withStatus(errors.isEmpty() ? Response.Status.OK : Response.Status.CONFLICT)
      .withBody(errors);
  }

  @DELETE
  @Path(apiV1Config)
  @Produces(MediaType.APPLICATION_JSON)
  public boolean apiV1ConfigDelete(@QueryParam(pConfigId) String configId) {
    return isValidId(configId) && delete(configRoot, configId);
  }

  @GET
  @Path(apiV1ConfigSelect)
  @Produces(MediaType.APPLICATION_JSON)
  public A4ConfigState apiV1ConfigSelectGet(@QueryParam(pConfigId) String configId) {
    if (configId != null && !configId.isEmpty() && !isValidId(configId)) {
      return new A4ConfigState();
    }
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

  @GET
  @Path(apiV1ConfigList)
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<A4Config> apiV1ConfigListGet() {
    return configList(configRoot, gson).collect(Collectors.toList());
  }

  @GET
  @Path(apiV1Stats)
  @Produces(MediaType.APPLICATION_JSON)
  public A4Stats apiV1StatsGet() {
    return service.stats();
  }

}
