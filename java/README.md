# Lab 1 – Concurrency in practice (Java)

Requires JDK 21+ (tested on OpenJDK 25). No build tool, only `javac`. Run all commands from the repository root.

## Build

```bash
javac -d java/out $(find java/src -name "*.java")
```

## Data

```bash
python3 number-generator.py            # numbers.txt, 50 000 000 integers
python3 verify.py                      # reference totals
```

## Run

| Task | Command |
|------|---------|
| 1a – split the file into byte ranges  | `java -cp java/out Main 1a <threads> [file]` |
| 1c – reader + bounded queue + workers | `java -cp java/out Main 1c <workers> [file]` |

Each run prints one line:

```
1a threads=8           negative=25005851 positive=24994122 zero=27 total=50000000 time=239ms peakHeap=9MB
```

`java/run.sh` builds and runs every configuration (1, 2, 4 … 64 threads), adding the peak RSS of the whole process
from `/usr/bin/time`. `peakHeap` covers Java objects only; `rss` also covers thread stacks and the JVM itself.

```bash
java/run.sh            # 1a and 1c
java/run.sh 1c         # one task
```

## Code

Each task lives in its own package; `common` holds what they share.

```
java/src/
├── Main.java                  command line, timing, output
├── common/                    shared by every task
│   ├── Counts.java            negative / positive / zero counters
│   ├── NumberReader.java      buffered parser of one integer per line
│   ├── Parallel.java          run a task on N OS threads, sum their Counts, split a range into equal parts
│   └── Memory.java            peak Java heap
├── task1a/
│   └── SplitFileCounter.java  each thread parses its own byte range of the file
└── task1c/
    └── ProducerConsumer.java  reader thread → ArrayBlockingQueue (100 × 10 000 lines) → workers
```

**Part boundaries (1a).** A line belongs to the part that holds its first byte. Each thread opens the file one byte
before its start and skips up to the next newline: that skips only the newline if the thread starts on a line start,
otherwise it skips the tail of the previous thread's line. It stops at the first line starting at or after its end.
