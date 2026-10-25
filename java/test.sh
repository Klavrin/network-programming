#!/usr/bin/env bash
# Correctness check for every task: its totals must equal the ones printed by verify.py.
# Task 1b "unsafe" is expected to be wrong (it shows the race condition), so it must NOT match.
#
#   java/test.sh              uses parts/ for Task 2
#   java/test.sh /Volumes/USB/parts
set -euo pipefail
cd "$(dirname "$0")/.."
javac -d java/out $(find java/src -name "*.java")

parts="${1:-parts}"
[[ -d "$parts" ]] || java -cp java/out Main split numbers.txt "$parts"

# "negative=… positive=… zero=…" from verify.py, in the same format the Java program prints.
expected=$(python3 verify.py | awk '/Negative/ {n=$2} /Positive/ {p=$2} /Zero/ {z=$2} END {print "negative="n" positive="p" zero="z}')
echo "expected: $expected"
failures=0

check() {
    local want="$1"; shift
    local line
    line=$(java -cp java/out Main "$@")
    if [[ "$line" == *"$expected"* ]]; then got=match; else got=mismatch; fi
    if [[ "$got" == "$want" ]]; then echo "PASS  $line"; else echo "FAIL  $line"; failures=$((failures + 1)); fi
}

for t in 1 2 4 8 16 32 64; do check match 1a "$t"; done
check mismatch 1b unsafe
check match 1b lock
check match 1b local
for t in 1 2 4 8 16 32 64; do check match 1c "$t"; done
for t in 1 2 4; do check match 2 "$t" "$parts"; done

echo "$failures failure(s)"
exit $((failures > 0))
