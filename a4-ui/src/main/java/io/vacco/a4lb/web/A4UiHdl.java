package io.vacco.a4lb.web;

import io.vacco.murmux.http.*;
import io.vacco.murmux.middleware.MxStatic;
import java.io.File;
import java.nio.file.Paths;

import static io.vacco.a4lb.web.A4Route.*;

public class A4UiHdl extends MxStatic {

  public static final String html = String.join("\n", "",
      "<!DOCTYPE html>",
      "<html>",
      "<head>",
      "  <base href=\"/\" />",
      "  <meta charset=\"utf-8\" />",
      "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">",
      "  <link rel=\"icon\" href=\"/favicon.svg\" type=\"image/svg+xml\">",
      "  <link rel=\"stylesheet\" href=\"/index.css\" />",
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

  private static final File pkgJson = new File("../a4-ui/package.json"); // TODO fix this, constructor should determine content origin.

  public A4UiHdl() {
    super(
        pkgJson.exists() ? Origin.FileSystem : Origin.Classpath,
        Paths.get("../a4-ui/build/resources/main/ui")
    );
    this.withNoTypeResolver((p, o) -> p.endsWith(".map") ? MxMime.json.type : MxMime.bin.type);
  }

  @Override public void handle(MxExchange xc) {
    var p = xc.getPath();
    if (p.startsWith(simpleIcons)) {
      super.handle(xc);
      return;
    }
    switch (p) {
      case indexCss:
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
