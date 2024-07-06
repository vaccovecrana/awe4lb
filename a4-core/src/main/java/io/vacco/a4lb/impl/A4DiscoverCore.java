package io.vacco.a4lb.impl;

import com.google.gson.Gson;
import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.util.A4Io;
import org.buildobjects.process.ProcBuilder;
import java.net.*;
import java.util.*;
import java.util.stream.*;

public class A4DiscoverCore {

  protected static A4Backend parseLine(String line) {
    var parts = line.split(" ");
    var sck = new A4Sock().host(parts[0]).port(Integer.parseInt(parts[1]));
    var bk = new A4Backend().addr(sck);
    if (parts.length == 4) {
      bk = bk.weight(Integer.parseInt(parts[2])).priority(Integer.parseInt(parts[3]));
    }
    return bk;
  }

  protected static List<A4Backend> parseLines(Stream<String> lines) {
    return lines
      .map(String::trim)
      .filter(line -> !line.isEmpty())
      .map(A4DiscoverCore::parseLine)
      .collect(Collectors.toList());
  }

  protected static List<A4Backend> parsePlainText(String out) {
    return parseLines(Arrays.stream(out.split("\\R")));
  }

  protected static List<A4Backend> parseOutput(String out, A4Format format, Gson gson) {
    if (format == A4Format.json) {
      var pool = gson.fromJson(out, A4Pool.class);
      return pool.hosts;
    } else if (format == A4Format.text) {
      return parsePlainText(out);
    }
    throw new IllegalArgumentException("Invalid output format " + format);
  }

  protected static List<A4Backend> execDiscover(A4Match match, Gson gson) {
    var d = match.discover;
    var x = match.discover.exec;
    var result = new ProcBuilder(x.command, x.args).withTimeoutMillis(d.timeoutMs).run();
    return parseOutput(result.getOutputString(), x.format, gson);
  }

  protected static List<A4Backend> httpDiscover(A4Match match, Gson gson) {
    try {
      var d = match.discover;
      var h = match.discover.http;
      var content = A4Io.loadContent(new URI(h.endpoint), d.timeoutMs);
      return parseOutput(content, h.format, gson);
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

}
