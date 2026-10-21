package io.vacco.a4lb.web;

import io.vacco.murmux.http.MxExchange;
import io.vacco.murmux.http.MxMime;
import io.vacco.murmux.middleware.MxStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;

import static io.vacco.a4lb.web.A4Route.*;

public class A4UiHdl extends MxStatic {

  private static final Logger log = LoggerFactory.getLogger(A4UiHdl.class);
  private static final String indexHtml = "/index.html";

  private static final File pkgJson = new File("../a4-ui/package.json");

  public A4UiHdl() {
    super(
      pkgJson.exists() ? Origin.FileSystem : Origin.Classpath,
      pkgJson.exists()
        ? Paths.get("../a4-ui/build/resources/main/ui")
        : Paths.get("/ui")
    );
    this.withNoTypeResolver((p, o) ->
      p.getFileName().toString().endsWith(".map") ? MxMime.json.type : MxMime.bin.type);
    log.info("ui - resources: ({}) - {}",
      pkgJson.exists() ? Origin.FileSystem : Origin.Classpath,
      pkgJson.exists() ? "../a4-ui/build/resources/main/ui" : "/ui");
  }

  @Override
  public void handle(MxExchange xc) {
    var p = xc.getPath();
    switch (p) {
      case indexHtml:
      case indexCss:
      case indexCssMap:
      case indexJs:
      case indexJsMap:
      case favicon:
        super.handle(xc);
        break;
      default: // any other Preact router path
        handleWithPath(xc, indexHtml);
    }
  }

}
