package io.vacco.a4lb;

import com.google.gson.Gson;
import io.vacco.a4lb.api.A4Controller;
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

  public A4Api(Gson g, A4Flags fl) {
    this.fl = Objects.requireNonNull(fl);
    this.mx = new Murmux(fl.api.host);
    var rpc = new RvMxAdapter<>(
        new A4Controller(), (xc, e) -> log.error("momo?", e),
        g::fromJson, g::toJson
    ).build();
    mx.rootHandler(
        new MxRouter()
            .prefix(A4Controller.apiV1Config, rpc)
            .noMatch(xc -> xc.withStatus(MxStatus._500).commitText("wat?"))
    );
  }

  public A4Api start() {
    mx.listen(fl.api.port);
    return this;
  }

  public void stop() {
    mx.stop();
  }

}
