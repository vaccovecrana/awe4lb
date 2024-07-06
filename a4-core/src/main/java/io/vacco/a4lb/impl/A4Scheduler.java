package io.vacco.a4lb.impl;

import am.ik.yavi.jsr305.NonNull;
import io.vacco.a4lb.util.A4Io;
import org.slf4j.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import static io.vacco.a4lb.util.A4Logging.onError;

public class A4Scheduler implements ThreadFactory {

  private static final Logger log = LoggerFactory.getLogger(A4Scheduler.class);
  private static final String issuesUrl = "https://github.com/vaccovecrana/awe4lb/issues";

  private final String id;

  private final ScheduledExecutorService scheduler;
  private final ExecutorService executor = Executors.newCachedThreadPool(this);

  private final BlockingQueue<Callable<?>> taskQueue = new LinkedBlockingQueue<>();
  private final ConcurrentHashMap<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
  private final List<A4Srv> servers = new ArrayList<>();

  public A4Scheduler(int schedulerCapacity, String id) {
    this.id = Objects.requireNonNull(id);
    this.scheduler = Executors.newScheduledThreadPool(schedulerCapacity);
  }

  public void schedulePermanent(A4Srv server) {
    this.servers.add(server);
    this.executor.submit(server);
  }

  public <T> void scheduleFixed(String taskId, long interval, TimeUnit unit,
                                Callable<T> task, Consumer<T> onResult) {
    if (scheduledTasks.containsKey(taskId)) {
      onError(log, "{} - task already scheduled", null, taskId);
    } else {
      var future = scheduler.scheduleAtFixedRate(() -> {
        try {
          taskQueue.put(() -> {
            var result = task.call();
            onResult.accept(result);
            return result;
          });
        } catch (InterruptedException e) {
          onError(log, "{} - scheduled task interrupted", e, taskId);
          Thread.currentThread().interrupt();
        }
      }, 0, interval, unit);
      scheduledTasks.put(taskId, future);
    }
  }

  private void processQueue() {
    var arr = new Object[1];
    while (true) {
      try {
        var task = taskQueue.take();
        arr[0] = task;
        executor.submit(task);
        arr[0] = null;
      } catch (InterruptedException e) {
        onError(log, "{} - queue task interrupted", e, arr[0] != null ? arr[0] : "?");
        Thread.currentThread().interrupt();
        break;
      }
    }
  }

  public boolean cancel(String taskId) {
    var future = scheduledTasks.remove(taskId);
    if (future != null) {
      return future.cancel(false);
    }
    return false;
  }

  public void start() {
    executor.submit(this::processQueue);
  }

  public void stop() {
    scheduler.shutdownNow();
    executor.shutdownNow();
    servers.forEach(A4Io::close);
    servers.clear();
    log.info("{} - stopped", this.id);
  }

  @Override public Thread newThread(@NonNull Runnable r) {
    var t = new Thread(r);
    t.setName(String.format("a4lb-%s-%x", this.id, t.hashCode()));
    t.setUncaughtExceptionHandler((t0, e) -> log.error(
      "{} - General task execution error. Please submit a bug at {}",
      t0.getName(), issuesUrl, e
    ));
    return t;
  }

}
