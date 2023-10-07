package io.vacco.a4lb.cfg;

import java.util.Collections;
import java.util.List;

public class A4HealthExec {

  public String command;
  public String[] args;

  public List<String> argList() {
    return args != null ? List.of(args) : Collections.emptyList();
  }

}
