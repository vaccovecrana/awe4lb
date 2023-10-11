package io.vacco.a4lb;

import com.google.gson.Gson;
import io.vacco.a4lb.api.*;
import io.vacco.a4lb.util.A4Flags;
import io.vacco.murmux.Murmux;
import io.vacco.murmux.http.MxStatus;
import io.vacco.murmux.middleware.MxRouter;
import io.vacco.ronove.murmux.RvMxAdapter;
import org.slf4j.*;
import java.util.Objects;

public class A4Api {

  private static final Logger log = LoggerFactory.getLogger(A4Api.class);

  private final Murmux mx;
  private final A4Flags fl;

  public A4Api(A4Service service, A4Flags fl,Gson g) {
    this.fl = Objects.requireNonNull(fl);
    this.mx = new Murmux(fl.api.host);
    var hdl = new A4Hdl(service);
    var rpc = new RvMxAdapter<>(hdl, (xc, e) -> log.error("momo?", e), g::fromJson, g::toJson)
        .build();
    mx.rootHandler(
        new MxRouter()
            .prefix(A4Route.apiV1Config, rpc)
            .noMatch(xc -> xc.withStatus(MxStatus._500).commitText("wat?"))
    );
  }

  public A4Api start() {
    mx.listen(fl.api.port);
    log.info("Management API/UI ready at http://{}:{}/ui", fl.api.host, fl.api.port);
    return this;
  }

  public void stop() {
    mx.stop();
  }

}
