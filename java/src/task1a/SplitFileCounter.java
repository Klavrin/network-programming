package task1a;

import common.Counts;
import common.NumberReader;
import common.Parallel;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Task 1a: each thread reads and parses only its own byte range of the file.
 *
 * A line belongs to the part that contains its first byte. A thread therefore
 * starts one byte early and skips to the end of that line: if the byte before
 * its start is a newline, it skips only that byte; otherwise it skips the tail
 * of a line owned by the previous thread. It stops at the first line that
 * starts at or after its end, which the next thread counts.
 */
public final class SplitFileCounter {

  public static Counts count(Path file, int threads) throws Exception {
    long size = Files.size(file);

    return Parallel.sum(threads, thread -> {
      long start = Parallel.partStart(size, thread, threads);
      long end = Parallel.partStart(size, thread + 1, threads);
      Counts counts = new Counts();

      try (
        NumberReader reader = NumberReader.open(file, Math.max(start - 1, 0))
      ) {
        if (start > 0) reader.skipLine();
        reader.countUntil(end, counts);
      }
      return counts;
    });
  }
}
