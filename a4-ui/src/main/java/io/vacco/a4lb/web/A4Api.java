package io.vacco.a4lb.web;

import com.google.gson.Gson;
import io.vacco.a4lb.service.A4Service;
import io.vacco.a4lb.util.A4Options;
import io.vacco.murmux.Murmux;
import io.vacco.murmux.http.MxStatus;
import io.vacco.murmux.middleware.MxRouter;
import io.vacco.ronove.murmux.RvMxAdapter;
import org.slf4j.*;
import java.io.*;
import java.util.Objects;

public class A4Api implements Closeable {

  private static final Logger log = LoggerFactory.getLogger(A4Api.class);

  private final Murmux mx;
  private final A4Options fl;

  public A4Api(File configRoot, A4Service service, A4Options fl, Gson g) {
    this.fl = Objects.requireNonNull(fl);
    this.mx = new Murmux(fl.api.host);
    var apiHdl = new A4ApiHdl(configRoot, service, g);
    var uiHdl = new A4UiHdl();
    var rpc = new RvMxAdapter<>(apiHdl, (xc, e) -> {
      if (log.isDebugEnabled()) {
        log.debug("ui - request handling error", e);
      } else {
        log.warn("ui - request handling error {}", e.getMessage());
      }
      xc.withStatus(MxStatus._500);
      xc.commit();
    }, g::fromJson, g::toJson).build();
    mx.rootHandler(new MxRouter().prefix(A4Route.apiRoot, rpc).noMatch(uiHdl));
  }

  public A4Api open() {
    mx.listen(fl.api.port);
    log.info("ui - ready at http://{}:{}", fl.api.host, fl.api.port);
    return this;
  }

  @Override public void close() {
    mx.stop();
    log.info("ui - stopped");
  }

}
