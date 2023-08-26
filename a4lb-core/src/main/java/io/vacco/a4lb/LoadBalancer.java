package io.vacco.a4lb;

import io.vacco.a4lb.cfg.A4Backend;
import io.vacco.a4lb.cfg.A4Sock;

import java.util.List;
import java.util.Random;

public class LoadBalancer {

  private static final Random random = new Random(1984);

  public static A4Backend getNextBackend(List<A4Backend> backends) {
    int tw = 0;
    for (var b : backends) {
      tw = tw + b.weight;
    }
    int rw = random.nextInt(tw);
    int cw = 0;
    for (var backend : backends) {
      cw += backend.weight;
      if (rw < cw) {
        return backend;
      }
    }
    return null;
  }

  public static void main(String[] args) {
    // Create a list of backend servers
    var backends = List.of(
        new A4Backend().addr(new A4Sock().host("bk00")).weight(1).priority(0),
        new A4Backend().addr(new A4Sock().host("bk01")).weight(2).priority(0)
    );

    // Perform weighted random selection multiple times
    int host1Count = 0;
    int host2Count = 0;
    int totalSelections = 10000;

    for (int i = 0; i < totalSelections; i++) {
      var bk = getNextBackend(backends);
      if (bk != null) {
        if (bk.addr.host.equals("bk00")) {
          host1Count++;
        } else if (bk.addr.host.equals("bk01")) {
          host2Count++;
        }
      }
    }

    var host1Probability = (double) host1Count / totalSelections;
    var host2Probability = (double) host2Count / totalSelections;

    System.out.println("Host1 probability: " + host1Probability);
    System.out.println("Host2 probability: " + host2Probability);
  }
}

