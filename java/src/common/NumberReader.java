package common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

/** Parses one integer per line from a byte stream and remembers how many bytes it has consumed. */
public final class NumberReader implements AutoCloseable {

  private final InputStream in;
  private final byte[] buffer = new byte[64 * 1024];
  private int index;
  private int length;
  private long position;

  public NumberReader(InputStream in) {
    this.in = in;
  }

  /** Opens the file with the reader placed at byte offset {@code start}. */
  public static NumberReader open(Path file, long start) throws IOException {
    FileChannel channel = FileChannel.open(file).position(start);
    NumberReader reader = new NumberReader(Channels.newInputStream(channel));
    reader.position = start;
    return reader;
  }

  public boolean hasNext() throws IOException {
    return peek() != -1;
  }

  /** Skips everything up to and including the next newline. */
  public void skipLine() throws IOException {
    int b;
    do b = read(); while (b != '\n' && b != -1);
  }

  /** Parses the current line as an integer and moves to the next line. */
  public int nextInt() throws IOException {
    boolean negative = peek() == '-';
    if (negative) read();

    int value = 0;
    for (int b = read(); b != '\n' && b != -1; b = read()) {
      if (b >= '0' && b <= '9') value = value * 10 + (b - '0');
    }
    return negative ? -value : value;
  }

  /** Counts every line that starts before byte offset {@code end}. */
  public void countUntil(long end, Counts counts) throws IOException {
    while (position < end && hasNext()) {
      counts.add(nextInt());
    }
  }

  private int peek() throws IOException {
    if (index == length) {
      length = Math.max(in.read(buffer), 0);
      index = 0;
    }
    return index < length ? buffer[index] : -1;
  }

  private int read() throws IOException {
    int b = peek();
    if (b != -1) {
      index++;
      position++;
    }
    return b;
  }

  @Override
  public void close() throws IOException {
    in.close();
  }
}
