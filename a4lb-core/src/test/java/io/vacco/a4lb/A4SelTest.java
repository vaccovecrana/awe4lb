package io.vacco.a4lb;

import com.google.gson.Gson;
import io.vacco.a4lb.cfg.A4Backend;
import io.vacco.a4lb.sel.A4Sel;
import io.vacco.a4lb.util.A4Configs;
import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;

import static j8spec.J8Spec.*;

@DefinedOrder
@RunWith(J8SpecRunner.class)
public class A4SelTest {
  static {
    it("Selects backend matches", () -> {
      var cfg = A4Configs.loadFrom(A4ValidTest.class.getResource("/config.json"), new Gson());
      var srv = cfg.servers.get("vacco-tls");
      srv.allBackends().forEach(bk -> bk.state = A4Backend.State.Up);
      var mat = new A4Sel(srv.match);
      var host = "172.16.0.111";
      var bk = mat.select(host, host.hashCode(), "momo.xio.vacco.li");
      System.out.println(bk);
    });
  }
}
