package task1b;

import common.Counts;
import common.NumberReader;
import common.Parallel;
import java.nio.file.Path;
import java.util.Arrays;

/** Task 1b: count an in-memory array three ways, to show a race condition and what fixing it costs. */
public final class ArrayCounter {

  public static int[] load(Path file) throws Exception {
    int[] numbers = new int[1 << 20];
    int size = 0;

    try (NumberReader reader = NumberReader.open(file, 0)) {
      while (reader.hasNext()) {
        if (size == numbers.length) numbers = Arrays.copyOf(numbers, size * 2);
        numbers[size++] = reader.nextInt();
      }
    }
    return Arrays.copyOf(numbers, size);
  }

  public static Counts count(int[] numbers, int threads, String mode)
    throws Exception {
    return switch (mode) {
      case "unsafe" -> sharedWithoutLock(numbers, threads);
      case "lock" -> sharedWithLock(numbers, threads);
      case "local" -> perThread(numbers, threads);
      default -> throw new IllegalArgumentException(
        "mode must be unsafe, lock or local: " + mode
      );
    };
  }

  /** 1. All threads update the same counters with no synchronisation: updates get lost. */
  private static Counts sharedWithoutLock(int[] numbers, int threads)
    throws Exception {
    Counts shared = new Counts();
    Parallel.run(threads, thread -> {
      for (
        int i = start(numbers, thread, threads);
        i < start(numbers, thread + 1, threads);
        i++
      ) {
        shared.add(numbers[i]);
      }
    });
    return shared;
  }

  /** 2. Same shared counters, but every update holds a lock: correct, and the threads queue up. */
  private static Counts sharedWithLock(int[] numbers, int threads)
    throws Exception {
    Counts shared = new Counts();
    Parallel.run(threads, thread -> {
      for (
        int i = start(numbers, thread, threads);
        i < start(numbers, thread + 1, threads);
        i++
      ) {
        synchronized (shared) {
          shared.add(numbers[i]);
        }
      }
    });
    return shared;
  }

  /** 3. Each thread has its own counters; they are added together once all threads finish. */
  private static Counts perThread(int[] numbers, int threads) throws Exception {
    return Parallel.sum(threads, thread -> {
      Counts own = new Counts();
      for (
        int i = start(numbers, thread, threads);
        i < start(numbers, thread + 1, threads);
        i++
      ) {
        own.add(numbers[i]);
      }
      return own;
    });
  }

  private static int start(int[] numbers, int part, int parts) {
    return (int) Parallel.partStart(numbers.length, part, parts);
  }
}
