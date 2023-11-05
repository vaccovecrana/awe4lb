package io.vacco.a4lb.cfg;

public class A4HealthCheck {

  public static final Integer
      DefaultIntervalMs = 5000,
      DefaultTimeoutMs = 2500;

  public Integer intervalMs, timeoutMs;
  public A4HealthExec exec;

  public A4HealthCheck intervalMs(Integer intervalMs) {
    this.intervalMs = intervalMs;
    return this;
  }

  public A4HealthCheck timeoutMs(Integer timeoutMs) {
    this.timeoutMs = timeoutMs;
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
