package common;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Small helpers for running work on N platform (OS) threads. */
public final class Parallel {

  public interface Task {
    void run(int thread) throws Exception;
  }

  public interface CountingTask {
    Counts count(int thread) throws Exception;
  }

  /** Runs {@code task(0..threads-1)}, each on its own thread, and waits for all of them. */
  public static void run(int threads, Task task) throws Exception {
    AtomicReference<Exception> failure = new AtomicReference<>();
    List<Thread> started = new ArrayList<>();

    for (int i = 0; i < threads; i++) {
      int thread = i;
      started.add(
        Thread.ofPlatform().start(() -> {
          try {
            task.run(thread);
          } catch (Exception e) {
            failure.compareAndSet(null, e);
          }
        })
      );
    }
    for (Thread t : started) t.join();

    if (failure.get() != null) throw failure.get();
  }

  /** Runs the task on every thread, each with its own counts, and adds them together at the end. */
  public static Counts sum(int threads, CountingTask task) throws Exception {
    Counts[] results = new Counts[threads];
    run(threads, thread -> results[thread] = task.count(thread));

    Counts total = new Counts();
    for (Counts counts : results) total.add(counts);
    return total;
  }

  /** Start of part {@code part} when {@code size} items are split into {@code parts} equal parts. */
  public static long partStart(long size, int part, int parts) {
    return (size * part) / parts;
  }
}
