package io.vacco.a4lb;

import io.vacco.murmux.http.*;
import io.vacco.murmux.middleware.MxStatic;
import java.io.File;
import java.nio.file.Paths;

import static io.vacco.a4lb.A4Route.*;

public class A4UiHdl extends MxStatic {

  public static final String html = String.join("\n", "",
      "<!DOCTYPE html>",
      "<html>",
      "<head>",
      "  <base href=\"/\" />",
      "  <meta charset=\"utf-8\" />",
      "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">",
      "</head>",
      "<body class=\"dark\">",
      "  <div id=\"root\" />",
      "  <script src=\"/index.js\"></script>",
      "  <noscript>",
      "    <!-- Happiness = Reality minus Expectations -->",
      "    <!-- :P -->",
      "  </noscript>",
      "  </body>",
      "</html>"
  );

  private static final File readme = new File("./README.md");

  public A4UiHdl() {
    super(
        readme.exists() ? Origin.FileSystem : Origin.Classpath,
        readme.exists() ? Paths.get("./build/resources/main/ui") : Paths.get("/ui")
    );
    this.withNoTypeResolver((p, o) -> p.endsWith(".map") ? MxMime.json.type : MxMime.bin.type);
  }

  @Override public void handle(MxExchange xc) {
    var p = xc.getPath();
    switch (p) {
      case indexJs:
      case indexJsMap:
      case favicon:
        super.handle(xc);
        break;
      default: // any other Preact router path
        xc.commitHtml(html);
    }
  }

}
