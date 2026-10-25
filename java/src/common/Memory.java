package common;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;

/** Peak Java heap usage, as reported by the JVM. Thread stacks are not heap; use the RSS from run.sh for those. */
public final class Memory {

  private static final long MB = 1024 * 1024;

  public static void resetPeak() {
    for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
      if (pool.getType() == MemoryType.HEAP) pool.resetPeakUsage();
    }
  }

  public static long peakHeapMb() {
    long bytes = 0;
    for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
      if (pool.getType() == MemoryType.HEAP) bytes += pool
        .getPeakUsage()
        .getUsed();
    }
    return bytes / MB;
  }
}
