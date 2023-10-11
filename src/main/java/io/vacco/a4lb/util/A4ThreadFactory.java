package io.vacco.a4lb.util;

import org.slf4j.*;
import java.util.Objects;
import java.util.concurrent.ThreadFactory;

public class A4ThreadFactory implements ThreadFactory {

  private static final Logger log = LoggerFactory.getLogger(A4ThreadFactory.class);
  private static final String issuesUrl = "https://github.com/vaccovecrana/awe4lb/issues";

  private final String prefix;

  public A4ThreadFactory(String prefix) {
    this.prefix = Objects.requireNonNull(prefix);
  }

  @Override public Thread newThread(Runnable r) {
    var t = new Thread(r);
    t.setName(String.format("%s-%x", prefix, t.hashCode()));
    t.setUncaughtExceptionHandler((t0, e) -> log.error(
        "{} - General task execution error. Please submit a bug at {}",
        t0.getName(), issuesUrl, e
    ));
    return t;
  }
}
