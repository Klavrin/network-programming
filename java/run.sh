#!/usr/bin/env bash
# Builds the program and runs every Task 1 configuration.
# Each line also shows the peak RSS of the whole process (heap + thread stacks + JVM), measured by /usr/bin/time.
#
#   java/run.sh           all of Task 1
#   java/run.sh 1a        only one task
#
# Task 2 is run by hand, because the USB drive must be re-plugged before every run:
#   java/run.sh 2 1 /Volumes/USB/parts
set -euo pipefail
cd "$(dirname "$0")/.."
javac -d java/out $(find java/src -name "*.java")

# Runs one configuration and appends its peak RSS (macOS `time -l` prints bytes, Linux `time -v` prints KB).
run() {
    local output
    if [[ "$(uname)" == "Darwin" ]]; then
        output=$( { /usr/bin/time -l java -cp java/out Main "$@"; } 2>&1 )
        rss=$(awk '/maximum resident set size/ { print int($1 / 1048576) }' <<< "$output")
    else
        output=$( { /usr/bin/time -v java -cp java/out Main "$@"; } 2>&1 )
        rss=$(awk -F: '/Maximum resident set size/ { print int($2 / 1024) }' <<< "$output")
    fi
    echo "$(head -n 1 <<< "$output") rss=${rss}MB"
}

THREADS=(1 2 4 8 16 32 64)
task="${1:-all}"

if [[ "$task" == "1a" || "$task" == "all" ]]; then
    for t in "${THREADS[@]}"; do run 1a "$t"; done
fi
if [[ "$task" == "1b" || "$task" == "all" ]]; then
    for i in 1 2 3; do run 1b unsafe; done
    run 1b lock
    run 1b local
fi
if [[ "$task" == "1c" || "$task" == "all" ]]; then
    for t in "${THREADS[@]}"; do run 1c "$t"; done
fi
if [[ "$task" == "2" ]]; then
    shift
    run 2 "$@"
fi
