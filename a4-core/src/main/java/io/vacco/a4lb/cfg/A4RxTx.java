package io.vacco.a4lb.cfg;

public class A4RxTx {

  // TODO not sure if these should be configurable parameters. I'm currently leaning towards no.
  public static final int AvgSize = 6;
  public static final long UpdateTimeMs = 500;

  public A4Avg
      rxAvg = new A4Avg().init(AvgSize),
      txAvg = new A4Avg().init(AvgSize);

  public long
      rxTimeMs = System.currentTimeMillis(),
      txTimeMs = System.currentTimeMillis();

  public boolean canUpdate(long t0, long t1) {
    return (t1 - t0) >= UpdateTimeMs;
  }

  public void updateRx(long rxBytes) {
    var nowMs = System.currentTimeMillis();
    if (canUpdate(rxTimeMs, nowMs)) {
      this.rxTimeMs = nowMs;
      rxAvg.update(rxBytes);
    }
  }

  public void updateTx(long txBytes) {
    var nowMs = System.currentTimeMillis();
    if (canUpdate(txTimeMs, nowMs)) {
      this.txTimeMs = nowMs;
      txAvg.update(txBytes);
    }
  }

}
