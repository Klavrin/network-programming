package task1c;

import common.Counts;
import common.Parallel;
import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/** Task 1c: one reader thread fills a bounded queue with batches of lines; worker threads count them. */
public final class ProducerConsumer {

  private static final int BATCH_SIZE = 10_000;
  private static final int QUEUE_CAPACITY = 100;

  /** "Queue closed" marker. Compared by identity, so it never collides with a real batch. */
  private static final List<String> END = new ArrayList<>();

  public static Counts count(Path file, int workers) throws Exception {
    BlockingQueue<List<String>> queue = new ArrayBlockingQueue<>(
      QUEUE_CAPACITY
    );

    Thread reader = Thread.ofPlatform().start(() -> {
      try {
        produce(file, queue);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    Counts counts = Parallel.sum(workers, worker -> consume(queue));
    reader.join();
    return counts;
  }

  /** put() blocks while the queue is full. */
  private static void produce(Path file, BlockingQueue<List<String>> queue)
    throws Exception {
    try (BufferedReader in = Files.newBufferedReader(file)) {
      List<String> batch = new ArrayList<>(BATCH_SIZE);
      for (String line = in.readLine(); line != null; line = in.readLine()) {
        batch.add(line);
        if (batch.size() == BATCH_SIZE) {
          queue.put(batch);
          batch = new ArrayList<>(BATCH_SIZE);
        }
      }
      if (!batch.isEmpty()) queue.put(batch);
    } finally {
      queue.put(END);
    }
  }

  /** take() blocks while the queue is empty. */
  private static Counts consume(BlockingQueue<List<String>> queue)
    throws Exception {
    Counts counts = new Counts();
    for (
      List<String> batch = queue.take();
      batch != END;
      batch = queue.take()
    ) {
      for (String line : batch) counts.add(Integer.parseInt(line.trim()));
    }
    queue.put(END); // put the marker back so the other workers stop too
    return counts;
  }
}
