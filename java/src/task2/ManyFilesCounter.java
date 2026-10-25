package task2;

import common.Counts;
import common.NumberReader;
import common.Parallel;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/** Task 2: a file is the unit of work. Each thread reads whole files into one byte array and counts them. */
public final class ManyFilesCounter {

  public static Counts count(Path directory, int threads) throws Exception {
    return count(directory, threads, 0);
  }

  /** {@code megabytesPerSecond > 0} emulates a slow device shared by every thread. */
  public static Counts count(
    Path directory,
    int threads,
    double megabytesPerSecond
  ) throws Exception {
    List<Path> files = listParts(directory);
    Bandwidth bandwidth =
      megabytesPerSecond > 0 ? new Bandwidth(megabytesPerSecond) : null;

    return Parallel.sum(threads, thread -> {
      Counts counts = new Counts();
      // Thread t takes files t, t + threads, t + 2*threads, ... which spreads them evenly.
      for (int i = thread; i < files.size(); i += threads) {
        byte[] bytes = read(files.get(i), bandwidth);
        try (
          NumberReader reader = new NumberReader(
            new ByteArrayInputStream(bytes)
          )
        ) {
          reader.countUntil(Long.MAX_VALUE, counts);
        }
      }
      return counts;
    });
  }

  private static byte[] read(Path file, Bandwidth bandwidth) throws Exception {
    if (bandwidth == null) return Files.readAllBytes(file);
    byte[] bytes = new byte[(int) Files.size(file)];
    try (
      InputStream in = new ThrottledInput(Files.newInputStream(file), bandwidth)
    ) {
      in.readNBytes(bytes, 0, bytes.length);
    }
    return bytes;
  }

  public static List<Path> listParts(Path directory) throws Exception {
    try (Stream<Path> paths = Files.list(directory)) {
      return paths
        .filter(p -> p.getFileName().toString().matches("part-\\d+\\.txt"))
        .sorted()
        .toList();
    }
  }
}
