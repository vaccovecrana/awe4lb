package io.vacco.a4lb;

import io.vacco.a4lb.tcp.A4TcpIo;
import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;

import static j8spec.J8Spec.*;
import static org.junit.Assert.*;

@DefinedOrder
@RunWith(J8SpecRunner.class)
public class A4TcpIoTest {
  static {
    it("Rounds numbers to powers of two", () -> {
      var n = 91000;
      n = A4TcpIo.roundToPowerOfTwo(n);
      assertEquals(131072, n);
      n = A4TcpIo.MaxBufferSize + 256;
      n = A4TcpIo.roundToPowerOfTwo(n);
      assertEquals(A4TcpIo.MaxBufferSize, n);
    });
  }
}
