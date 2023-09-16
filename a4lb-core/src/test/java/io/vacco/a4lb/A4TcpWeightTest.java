package io.vacco.a4lb;

import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.tcp.A4TcpWeight;
import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;
import java.util.List;

import static j8spec.J8Spec.*;
import static org.junit.Assert.*;

@DefinedOrder
@RunWith(J8SpecRunner.class)
public class A4TcpWeightTest {
  static {
    it("Performs weighted random backend selection", () -> {
      // Create a list of backend servers
      var backends = List.of(
          new A4Backend().addr(new A4Sock().host("bk00")).weight(1).priority(0).state(A4Backend.State.Up),
          new A4Backend().addr(new A4Sock().host("bk01")).weight(1).priority(0).state(A4Backend.State.Up)
      );
      var pool = new A4Pool().hosts(backends);

      // Perform weighted random selection multiple times
      int host1Count = 0;
      int host2Count = 0;
      int totalSelections = 10000;

      for (int i = 0; i < totalSelections; i++) {
        var bk = A4TcpWeight.select(pool);
        if (bk.addr.host.equals("bk00")) {
          host1Count++;
        } else if (bk.addr.host.equals("bk01")) {
          host2Count++;
        }
      }

      var h1Prob = (double) host1Count / totalSelections;
      var h2Prob = (double) host2Count / totalSelections;

      System.out.println("Host1 probability: " + h1Prob);
      System.out.println("Host2 probability: " + h2Prob);

      assertEquals(0.5, h1Prob, 0.1);
      assertEquals(0.5, h2Prob, 0.1);
    });
  }
}
