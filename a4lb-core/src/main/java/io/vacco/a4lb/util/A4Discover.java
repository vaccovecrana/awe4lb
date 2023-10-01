package io.vacco.a4lb.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.vacco.a4lb.cfg.*;
import io.vacco.a4lb.sel.A4Sel;
import org.buildobjects.process.ProcBuilder;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.Callable;

public class A4Discover implements Callable<Void> {

  private final Type bkList = new TypeToken<ArrayList<A4Backend>>(){}.getType();
  private final A4Match match;
  private final A4Sel bkSel;
  private final Gson gson;

  public A4Discover(A4Match match, A4Sel bkSel, Gson gson) {
    this.match = Objects.requireNonNull(match);
    this.bkSel = Objects.requireNonNull(bkSel);
    this.gson = Objects.requireNonNull(gson);
    Objects.requireNonNull(match.discover);
  }

  @Override public Void call() {
    while (true) {
      if (match.discover.exec != null) {
        var x = match.discover.exec;
        var result = new ProcBuilder(x.command, x.args)
            .withTimeoutMillis(x.timeoutMs).run();
        var out = result.getOutputString();
        List<A4Backend> backends = gson.fromJson(out, bkList);
        for (var bk : backends) {
          var errors = A4Valid.A4BackendVld.validate(bk);
          if (!errors.isEmpty()) {
            throw new A4Exceptions.A4ConfigException(errors);
          }
        }
      } else if (disc.http != null) {
        return null;
      }
    }
  }

}
