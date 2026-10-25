import common.Counts;
import common.Memory;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import task1a.SplitFileCounter;
import task1b.ArrayCounter;
import task1c.ProducerConsumer;
import task2.ManyFilesCounter;
import task2.Splitter;

/**
 * Lab 1 entry point. Every task and configuration is chosen on the command line:
 *
 *   1a    <threads> [file]
 *   1b    <unsafe|lock|local> [threads] [file]
 *   1c    <workers> [file]
 *   2     <threads> <directory> [MB/s]
 *   split [file] [directory] [parts]
 */
public final class Main {

  private static final String DEFAULT_FILE = "numbers.txt";

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      usage();
      return;
    }

    switch (args[0]) {
      case "1a" -> {
        int threads = Integer.parseInt(args[1]);
        Path file = Path.of(arg(args, 2, DEFAULT_FILE));
        measure("1a threads=" + threads, () ->
          SplitFileCounter.count(file, threads)
        );
      }
      case "1b" -> {
        String mode = args[1];
        int threads = Integer.parseInt(
          arg(args, 2, "" + Runtime.getRuntime().availableProcessors())
        );
        int[] numbers = ArrayCounter.load(Path.of(arg(args, 3, DEFAULT_FILE))); // not measured
        measure("1b " + mode + " threads=" + threads, () ->
          ArrayCounter.count(numbers, threads, mode)
        );
      }
      case "1c" -> {
        int workers = Integer.parseInt(args[1]);
        Path file = Path.of(arg(args, 2, DEFAULT_FILE));
        measure("1c workers=" + workers, () ->
          ProducerConsumer.count(file, workers)
        );
      }
      case "2" -> {
        int threads = Integer.parseInt(args[1]);
        Path directory = Path.of(args[2]);
        double megabytesPerSecond = args.length > 3
          ? Double.parseDouble(args[3])
          : 0;
        String limit =
          megabytesPerSecond > 0 ? " limit=" + megabytesPerSecond + "MB/s" : "";
        measure("2 threads=" + threads + limit, () ->
          ManyFilesCounter.count(directory, threads, megabytesPerSecond)
        );
      }
      case "split" -> Splitter.split(
        Path.of(arg(args, 1, DEFAULT_FILE)),
        Path.of(arg(args, 2, "parts")),
        Integer.parseInt(arg(args, 3, "8"))
      );
      default -> usage();
    }
  }

  private static void measure(String label, Callable<Counts> work)
    throws Exception {
    Memory.resetPeak();
    long start = System.nanoTime();
    Counts counts = work.call();
    long millis = (System.nanoTime() - start) / 1_000_000;

    System.out.printf(
      "%-22s %s time=%dms peakHeap=%dMB%n",
      label,
      counts,
      millis,
      Memory.peakHeapMb()
    );
  }

  private static String arg(String[] args, int index, String fallback) {
    return index < args.length ? args[index] : fallback;
  }

  private static void usage() {
    System.out.println(
      """
      usage: java -cp java/out Main <task> ...
        1a    <threads> [file]                    split the file into byte ranges
        1b    <unsafe|lock|local> [threads] [file] count an in-memory array
        1c    <workers> [file]                    reader thread + bounded queue + workers
        2     <threads> <directory> [MB/s]        whole files (part-*.txt) as the unit of work
        split [file] [directory] [parts]          make part-1.txt ... part-8.txt for Task 2"""
    );
  }
}
