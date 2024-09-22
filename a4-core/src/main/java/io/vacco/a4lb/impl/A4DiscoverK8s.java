package io.vacco.a4lb.impl;

import com.google.gson.*;
import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.niossl.SSLCertificates;
import io.vacco.a4lb.util.A4Io;

import java.io.File;
import java.net.*;
import java.net.http.HttpClient;
import java.util.*;
import java.util.function.Function;

import static java.lang.String.format;

public class A4DiscoverK8s {

  private static String httpGet(HttpClient client, String urlStr, String authToken) {
    try {
      return A4Io.loadContent(new URI(urlStr), client, authToken, 3000);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static List<A4Backend> nodesOf(String apiServerUri, String serviceJson,
                                        String namespace, String serviceName, int port,
                                        Function<String, String> podJsonFn,
                                        Function<String, String> nodeJsonFn) {
    var nodes = new ArrayList<A4Backend>();
    var service = JsonParser.parseString(serviceJson).getAsJsonObject();
    var spec = service.getAsJsonObject("spec");

    if (spec.get("type").getAsString().equals("NodePort")) {
      var ports = spec.getAsJsonArray("ports");
      if (ports.isEmpty()) {
        throw new IllegalStateException(format("no ports found for service [%s, %s]", namespace, serviceName));
      }

      int nodePort = 0;
      var portFound = false;
      for (var p : ports) {
        var portMap = p.getAsJsonObject();
        if (portMap.get("port").getAsInt() == port) {
          nodePort = portMap.get("nodePort").getAsInt();
          portFound = true;
          break;
        }
      }
      if (!portFound) {
        throw new IllegalStateException(
          format("specified port not found for service [%s, %s, %s]", namespace, serviceName, port)
        );
      }

      var podsUri = format("%s/api/v1/namespaces/%s/pods?labelSelector=app=%s", apiServerUri, namespace, serviceName);
      var pods = JsonParser.parseString(podJsonFn.apply(podsUri)).getAsJsonObject();
      var items = pods.getAsJsonArray("items");
      for (var item : items) {
        var pod = item.getAsJsonObject();
        var podSpec = pod.getAsJsonObject("spec");
        if (podSpec != null && podSpec.get("nodeName") != null) {
          var nodeName = podSpec.get("nodeName").getAsString();
          var nodeUri = format("%s/api/v1/nodes/%s", apiServerUri, nodeName);
          var node = JsonParser.parseString(nodeJsonFn.apply(nodeUri)).getAsJsonObject();
          var status = node.getAsJsonObject("status");
          var addresses = status.getAsJsonArray("addresses");
          String nodeIp = null;
          for (var address : addresses) {
            var addr = address.getAsJsonObject();
            if (addr.get("type").getAsString().equals("InternalIP")) {
              nodeIp = addr.get("address").getAsString();
              break;
            }
          }
          nodes.add(
            new A4Backend()
              .state(A4BackendState.Unknown)
              .addr(new A4Sock().host(nodeIp).port(nodePort))
              .weight(1).priority(1)
          );
        }
      }
    }

    return nodes;
  }

  private static List<A4Backend> nodesOf(HttpClient client, String apiServerUri, String authToken,
                                         String namespace, String serviceName, int port) {
    if (authToken == null || authToken.isEmpty()) {
      throw new IllegalArgumentException("Authentication token not found");
    }

    var svcURL = format("%s/api/v1/namespaces/%s/services/%s", apiServerUri, namespace, serviceName);
    var serviceJson = httpGet(client, svcURL, authToken);
    var jsonUriFn = (Function<String, String>) uri -> httpGet(client, uri, authToken);

    return nodesOf(
      apiServerUri, serviceJson, namespace, serviceName, port,
      jsonUriFn, jsonUriFn
    );
  }

  public static List<A4Backend> k8sDiscover(A4Match match, HttpClient client) {
    var k = match.discover.k8s;
    var f = new File(k.tokenPath);
    var token = A4Io.loadContent(f.toURI(), 3000);
    return nodesOf(client, k.apiUri, token, k.namespace, k.service, k.port);
  }

}
