package common;

/** How many negative, positive and zero numbers were seen. Not thread-safe on purpose (see Task 1b). */
public final class Counts {

  private long negative;
  private long positive;
  private long zero;

  public void add(int number) {
    if (number < 0) negative++;
    else if (number > 0) positive++;
    else zero++;
  }

  public void add(Counts other) {
    negative += other.negative;
    positive += other.positive;
    zero += other.zero;
  }

  public long total() {
    return negative + positive + zero;
  }

  @Override
  public String toString() {
    return "negative=%d positive=%d zero=%d total=%d".formatted(
      negative,
      positive,
      zero,
      total()
    );
  }
}
