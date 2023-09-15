package io.vacco.a4lb;

import com.google.gson.Gson;
import io.vacco.a4lb.tcp.A4TcpMatch;
import io.vacco.a4lb.util.A4Configs;
import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;

import static j8spec.J8Spec.*;

@DefinedOrder
@RunWith(J8SpecRunner.class)
public class A4TcpMatchTest {
  static {
    it("Selects backend matches", () -> {
      var cfg = A4Configs.loadFrom(A4ValidTest.class.getResource("/config.json"), new Gson());

      var srv = cfg.servers.get("vacco-tls");
      var mat = new A4TcpMatch(srv.match);
      var bk = mat.select("172.16.0.111", "momo.xio.vacco.li");
      System.out.println(bk);
    });
  }
}
