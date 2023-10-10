package io.vacco.a4lb.api;

import com.google.gson.Gson;
import io.vacco.murmux.Murmux;
import io.vacco.murmux.http.MxStatus;
import io.vacco.murmux.middleware.MxRouter;
import io.vacco.ronove.murmux.RvMxAdapter;
import org.slf4j.*;

public class A4Api {

  private static final Logger log = LoggerFactory.getLogger(A4Api.class);

  private final Murmux mx;

  public A4Api(Gson g) {
    this.mx = new Murmux();
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
    mx.listen(7070);
    return this;
  }

  public void stop() {
    mx.stop();
  }

}
