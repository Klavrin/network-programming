import common.Counts;
import common.Memory;
import task1a.SplitFileCounter;
import task1c.ProducerConsumer;

import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Lab 1 entry point. Every task and configuration is chosen on the command line:
 *
 *   1a    <threads> [file]
 *   1c    <workers> [file]
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
                measure("1a threads=" + threads, () -> SplitFileCounter.count(file, threads));
            }
            case "1c" -> {
                int workers = Integer.parseInt(args[1]);
                Path file = Path.of(arg(args, 2, DEFAULT_FILE));
                measure("1c workers=" + workers, () -> ProducerConsumer.count(file, workers));
            }
            default -> usage();
        }
    }

    private static void measure(String label, Callable<Counts> work) throws Exception {
        Memory.resetPeak();
        long start = System.nanoTime();
        Counts counts = work.call();
        long millis = (System.nanoTime() - start) / 1_000_000;

        System.out.printf("%-22s %s time=%dms peakHeap=%dMB%n", label, counts, millis, Memory.peakHeapMb());
    }

    private static String arg(String[] args, int index, String fallback) {
        return index < args.length ? args[index] : fallback;
    }

    private static void usage() {
        System.out.println("""
                usage: java -cp java/out Main <task> ...
                  1a    <threads> [file]                    split the file into byte ranges
                  1c    <workers> [file]                    reader thread + bounded queue + workers""");
    }
}
