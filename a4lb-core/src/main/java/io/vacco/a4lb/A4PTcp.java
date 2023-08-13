package io.vacco.a4lb;

import stormpot.Pooled;
import stormpot.Slot;
import java.net.Socket;

public class A4PTcp extends Pooled<Socket> {
  public A4PTcp(Slot slot, Socket object) {
    super(slot, object);
  }
}
