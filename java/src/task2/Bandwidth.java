package task2;

import java.util.concurrent.locks.LockSupport;

/** One limiter shared by every thread: models a device whose total bandwidth is fixed. */
final class Bandwidth {

  private final double nanosPerByte;
  private long nextFreeNanos = System.nanoTime();

  Bandwidth(double megabytesPerSecond) {
    this.nanosPerByte = 1e9 / (megabytesPerSecond * 1e6);
  }

  /** Blocks until {@code bytes} more bytes may have been transferred. */
  void take(int bytes) {
    long wait;
    synchronized (this) {
      long now = System.nanoTime();
      if (nextFreeNanos < now) nextFreeNanos = now;
      wait = nextFreeNanos - now;
      nextFreeNanos += (long) (bytes * nanosPerByte);
    }
    long deadline = System.nanoTime() + wait;
    for (long left; (left = deadline - System.nanoTime()) > 0; ) {
      LockSupport.parkNanos(left);
    }
  }
}
