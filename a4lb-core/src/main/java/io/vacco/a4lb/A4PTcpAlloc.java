package io.vacco.a4lb;

import stormpot.*;
import java.net.Socket;

public class A4PTcpAlloc implements Allocator<A4PTcp> {

  @Override public A4PTcp allocate(Slot s) throws Exception {
    // TODO gobetween's allocation strategies may go here. But for now just return dummy backend socket.
    return new A4PTcp(s, new Socket("127.0.0.1", 6900));
  }

  @Override public void deallocate(A4PTcp ps) {
    A4Io.close(ps.object);
  }

}
