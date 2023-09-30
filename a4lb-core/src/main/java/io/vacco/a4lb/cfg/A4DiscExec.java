package io.vacco.a4lb.cfg;

import java.util.Collections;
import java.util.List;

public class A4DiscExec {

  public String command, args;
  public A4Format format;

  public List<String> argList() {
    return args != null ? List.of(args) : Collections.emptyList();
  }

}
