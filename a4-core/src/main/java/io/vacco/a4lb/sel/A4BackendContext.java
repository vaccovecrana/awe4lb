package io.vacco.a4lb.sel;

import io.vacco.a4lb.cfg.A4RxTx;

public class A4BackendContext {

  public A4RxTx rxTx = new A4RxTx();
  public int    connections = 0;

  public void trackConn(boolean isConnect) {
    if (isConnect) {
      this.connections = this.connections + 1;
    } else {
      this.connections = this.connections - 1;
    }
  }

  public void trackRxTx(boolean isRx, int bytes) {
    if (isRx) {
      rxTx.updateRx(bytes);
    } else {
      rxTx.updateTx(bytes);
    }
  }

}
