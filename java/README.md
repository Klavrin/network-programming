# Lab 1 – Concurrency in practice (Java)

Requires JDK 21+ (tested on OpenJDK 25). No build tool, only `javac`. Run all commands from the repository root.

## Build

```bash
javac -d java/out $(find java/src -name "*.java")
```

## Data

```bash
python3 number-generator.py            # numbers.txt, 50 000 000 integers
java -cp java/out Main split           # parts/part-1.txt ... part-8.txt, 6 250 000 each (Task 2)
python3 verify.py                      # reference totals
```

## Quick start

```bash
./lab.sh -cli     # arrow-key menu: Task 1a, 1b, 1c, all of Task 1, or Task 2 (↑/↓ or j/k, Enter, q)
./lab.sh 1        # or directly: 1a | 1b | 1c | 1 | 2 | 2 /Volumes/USB/parts
```

`lab.sh` holds only the menu entries and the commands they run (`java/run.sh`, `java/sim-usb.sh`, described below).
The reusable arrow-key menu widget is in `cli/menu.sh`.

## Run

| Task | Command |
|------|---------|
| 1a – split the file into byte ranges | `java -cp java/out Main 1a <threads> [file]` |
| 1b – count an in-memory array        | `java -cp java/out Main 1b <unsafe\|lock\|local> [threads] [file]` |
| 1c – reader + bounded queue + workers | `java -cp java/out Main 1c <workers> [file]` |
| 2  – whole files from a USB drive    | `java -cp java/out Main 2 <threads> <directory>` |

`threads` in 1b defaults to the number of logical processors. Each run prints one line:

```
1a threads=8           negative=25005851 positive=24994122 zero=27 total=50000000 time=239ms peakHeap=9MB
```

`java/run.sh` builds and runs every Task 1 configuration (1, 2, 4 … 64 threads; `unsafe` three times), adding the
peak RSS of the whole process from `/usr/bin/time`. `peakHeap` covers Java objects only; `rss` also covers thread
stacks and the JVM itself.

```bash
java/run.sh            # all of Task 1
java/run.sh 1c         # one task
java/run.sh 2 4 /Volumes/USB/parts   # Task 2: re-plug the drive before every run
```

### Task 2 without a USB drive

`java/sim-usb.sh` (macOS, no sudo) puts `parts/` in a read-only disk image and mounts it with
`hdiutil` from a local HTTP server (`java/usb-server.py`) that has a single bandwidth cap. The program
reads a real mounted volume with the unchanged `Main 2`, and all threads share one bus speed.
Detaching and reattaching the image before each run empties its file cache, which stands in for
unplugging the drive. Spotlight cannot index the volume.

```bash
java/sim-usb.sh up 25        # build image, start server, mount; 25 MB/s ≈ a USB 2.0 stick
java/sim-usb.sh bench        # 1, 2, 4 threads, re-plugging before each
java/sim-usb.sh run 2        # a single configuration
java/sim-usb.sh down         # unmount + stop server   (clean: also delete the image)
java/test.sh .sim-usb/mnt/parts   # correctness of Task 2 on the simulated drive
```

## Test

`java/test.sh` runs every task and compares its totals with `verify.py`; `1b unsafe` must differ (race condition).
It prints `PASS` or `FAIL` per run and exits non-zero on any failure.

```bash
java/test.sh                      # Task 2 from parts/ (made automatically if missing)
java/test.sh /Volumes/USB/parts    # Task 2 from the USB drive
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
├── task1b/
│   └── ArrayCounter.java      unsafe / lock / local counting of an in-memory array
├── task1c/
│   └── ProducerConsumer.java  reader thread → ArrayBlockingQueue (100 × 10 000 lines) → workers
└── task2/
    ├── Splitter.java          makes parts/part-1.txt ... part-8.txt
    └── ManyFilesCounter.java  each thread reads whole files with Files.readAllBytes
```

**Part boundaries (1a).** A line belongs to the part that holds its first byte. Each thread opens the file one byte
before its start and skips up to the next newline: that skips only the newline if the thread starts on a line start,
otherwise it skips the tail of the previous thread's line. It stops at the first line starting at or after its end.
