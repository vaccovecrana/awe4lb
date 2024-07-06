package io.vacco.a4lb;

import io.vacco.a4lb.impl.A4DiscoverK8s;
import io.vacco.a4lb.util.A4Io;
import io.vacco.shax.logging.ShOption;
import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Function;

import static j8spec.J8Spec.*;

@DefinedOrder
@RunWith(J8SpecRunner.class)
public class A4DiscoverK8sTest {

  static {
    ShOption.setSysProp(ShOption.IO_VACCO_SHAX_DEVMODE, "true");
    ShOption.setSysProp(ShOption.IO_VACCO_SHAX_LOGLEVEL, "debug");
  }

  private static String load(String classPath) throws URISyntaxException {
    var url = A4DiscoverK8sTest.class.getResource(classPath);
    return A4Io.loadContent(Objects.requireNonNull(url).toURI(), 1000);
  }

  static {
    it("Finds K8s service backends",  () -> {
      var jsonIdx = new HashMap<String, String>();
      jsonIdx.put("/api/v1/namespaces/grayhawk/services/cockroachdb", load("/discover/k8s-service.json"));
      jsonIdx.put("/api/v1/namespaces/grayhawk/pods?labelSelector=app=cockroachdb", load("/discover/k8s-pod-list.json"));
      jsonIdx.put("/api/v1/nodes/tarkus-wrk-022", load("/discover/k8s-node-022.json"));
      jsonIdx.put("/api/v1/nodes/tarkus-wrk-030", load("/discover/k8s-node-030.json"));
      jsonIdx.put("/api/v1/nodes/tarkus-wrk-032", load("/discover/k8s-node-032.json"));

      var jsonFn = (Function<String, String>) jsonIdx::get;
      var nodes = A4DiscoverK8s.nodesOf("",
        load("/discover/k8s-service.json"),
        "grayhawk", "cockroachdb", 8080,
        jsonFn, jsonFn
      );

      for (var node : nodes) {
        System.out.println(node);
      }
    });
  }
}
