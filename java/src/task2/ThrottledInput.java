package task2;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Delivers bytes at the rate of a shared {@link Bandwidth}. */
final class ThrottledInput extends FilterInputStream {

  private final Bandwidth bandwidth;

  ThrottledInput(InputStream in, Bandwidth bandwidth) {
    super(in);
    this.bandwidth = bandwidth;
  }

  @Override
  public int read() throws IOException {
    int b = super.read();
    if (b >= 0) bandwidth.take(1);
    return b;
  }

  @Override
  public int read(byte[] buffer, int offset, int length) throws IOException {
    int n = super.read(buffer, offset, length);
    if (n > 0) bandwidth.take(n);
    return n;
  }
}
