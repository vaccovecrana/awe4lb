package io.vacco.a4lb;

import org.slf4j.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class A4PTcpMon {

  private static final Logger log = LoggerFactory.getLogger(A4PTcpMon.class);

  private final Random rnd = new Random();
  private final Map<String, Thread> cidIdx = new ConcurrentHashMap<>();
  private final ExecutorService es = Executors.newFixedThreadPool(
      4, r -> new Thread(r, String.format("a4lb-tcp-%x", rnd.nextInt()))
  );

  private Callable<Long> register(String cid, Socket in, Socket out) {
    return () -> {
      cidIdx.put(cid, Thread.currentThread());
      return A4Io.io(cid, in, out);
    };
  }

  private void drain(InputStream is) {
    try {
      int av = is.available();
      if (av > 0) {
        is.skip(av);
      }
    } catch (IOException ioe) {
      throw new IllegalStateException(ioe);
    }
  }

  public void accept(Socket client, A4PTcp backend) {
    var c2bCid = A4Io.connId(client, backend.object);
    var b2cCid = A4Io.connId(backend.object, client);
    try {
      es.invokeAny(List.of(
          register(c2bCid, client, backend.object),
          register(b2cCid, backend.object, client)
      ));
      var t0 = cidIdx.get(c2bCid);
      var t1 = cidIdx.get(b2cCid);

      t0.interrupt();
      t1.interrupt();

      A4Io.close(client);
      drain(backend.object.getInputStream());
      backend.release();
      if (log.isDebugEnabled()) {
        log.debug("TCP I/O monitor finished: [{}, {}]", c2bCid, b2cCid);
      }
    } catch (Exception e) {
      log.error("TCP I/O monitor error: [{}, {}]", c2bCid, b2cCid, e);
    }
  }
}
