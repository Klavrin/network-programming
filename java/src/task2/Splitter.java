package task2;

import common.Parallel;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/** Prepares Task 2: splits the input, in order, into equal files part-1.txt ... part-N.txt. */
public final class Splitter {

  public static void split(Path file, Path directory, int parts)
    throws Exception {
    long lines;
    try (Stream<String> all = Files.lines(file)) {
      lines = all.count();
    }
    Files.createDirectories(directory);

    try (BufferedReader in = Files.newBufferedReader(file)) {
      for (int part = 0; part < parts; part++) {
        long count =
          Parallel.partStart(lines, part + 1, parts) -
          Parallel.partStart(lines, part, parts);
        Path target = directory.resolve("part-" + (part + 1) + ".txt");

        try (BufferedWriter out = Files.newBufferedWriter(target)) {
          for (long i = 0; i < count; i++) {
            out.write(in.readLine());
            out.newLine();
          }
        }
        System.out.println("wrote " + target + " (" + count + " numbers)");
      }
    }
  }
}
