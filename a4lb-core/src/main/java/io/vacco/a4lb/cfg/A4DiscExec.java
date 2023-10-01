package io.vacco.a4lb.cfg;

import java.util.Collections;
import java.util.List;

public class A4DiscExec {

  public int timeoutMs;
  public String command;
  public String[] args;
  public A4Format format;

  public A4DiscExec command(String command) {
    this.command = command;
    return this;
  }

  public A4DiscExec args(String ... args) {
    this.args = args;
    return this;
  }

  public A4DiscExec timeoutMs(int timeoutMs) {
    this.timeoutMs = timeoutMs;
    return this;
  }

  public A4DiscExec format(A4Format format) {
    this.format = format;
    return this;
  }

  public List<String> argList() {
    return args != null ? List.of(args) : Collections.emptyList();
  }

}
