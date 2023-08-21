package io.vacco.a4lb.cfg;

public class A4Exec {

  public String command;    // a custom script to invoke with [host, port] as arguments.
  public String passOutput; // if the script produces this output, the host is marked healthy.
  public String failOutput; // if the script produces this output, the host is marked unhealthy.

  public A4Exec command(String command) {
    this.command = command;
    return this;
  }

  public A4Exec passOutput(String passOutput) {
    this.passOutput = passOutput;
    return this;
  }

  public A4Exec failOutput(String failOutput) {
    this.failOutput = failOutput;
    return this;
  }

}
