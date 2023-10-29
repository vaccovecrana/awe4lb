package io.vacco.a4lb;

import com.google.gson.Gson;
import io.vacco.a4lb.cfg.A4Backend;
import io.vacco.a4lb.sel.A4Selector;
import io.vacco.a4lb.util.A4Configs;
import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;

import static j8spec.J8Spec.*;

@DefinedOrder
@RunWith(J8SpecRunner.class)
public class A4SelectorTest {
  static {
    it("Selects backend matches", () -> {
      var cfg = A4Configs.loadFrom(A4SelectorTest.class.getResource("/test-config-00.json"), new Gson());
      cfg.servers.stream()
          .flatMap(srv -> srv.match.stream())
          .flatMap(m -> m.pool.hosts.stream())
          .forEach(bk -> bk.state = A4Backend.State.Up);
      var matches = cfg.servers.get(3).match;
      var sel = new A4Selector(matches);
      var host = "172.16.0.111";
      var bk = sel.select(matches.get(0).pool, host.hashCode());
      System.out.println(bk);
    });
  }
}
