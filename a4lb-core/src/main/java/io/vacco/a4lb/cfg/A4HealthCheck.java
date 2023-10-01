package io.vacco.a4lb.cfg;

public class A4HealthCheck {

  public int intervalMs = 5000, timeoutMs = 4900;
  public A4Exec exec;

  public A4HealthCheck intervalMs(int intervalMs) {
    this.intervalMs = intervalMs;
    return this;
  }

  public A4HealthCheck timeoutMs(int timeoutMs) {
    this.timeoutMs = timeoutMs;
    return this;
  }

  public A4HealthCheck exec(A4Exec exec) {
    this.exec = exec;
    return this;
  }

  /*
   * < You have heard Wrong/Fake News >
   * < The Board is A-OK/Intact >
   * < The Rebel Faction/Dissent is >
   * < Former/Fired >
   * < The Crisis/Purge is over >
   * < This is not a Matter/Worry for you >
   * < You can hang up now, please >
   */

}
