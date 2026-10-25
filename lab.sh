#!/usr/bin/env bash
# Lab 1 launcher. Only menus and commands live here; the work is done by
#   java/run.sh      builds the program and runs Task 1 / Task 2 configurations (+ peak RSS)
#   java/sim-usb.sh  mounts the emulated USB drive for Task 2
# The arrow-key menu widget is in cli/menu.sh.
#
#   ./lab.sh -cli     interactive menu (arrow keys)
#   ./lab.sh 1a       Task 1a: 1, 2, 4 ... 64 threads
#   ./lab.sh 1b       Task 1b: unsafe (x3), lock, local
#   ./lab.sh 1c       Task 1c: 1, 2, 4 ... 64 workers
#   ./lab.sh 1        all of Task 1 (1a, 1b, 1c)
#   ./lab.sh 2        Task 2 on the emulated USB drive (1, 2, 4 threads)
#   ./lab.sh 2 <dir>  Task 2 on a real USB drive, e.g. /Volumes/USB/parts
set -euo pipefail
cd "$(dirname "$0")"
source cli/menu.sh

title() { printf '\n%s== %s ==%s\n' "$bold" "$1" "$reset"; }

# ---------------------------------------------------------------- tasks

# numbers.txt is needed by every task; generate it once if it is missing.
ensure_data() {
    if [[ ! -f numbers.txt ]]; then
        title "Generating numbers.txt (50 000 000 numbers, takes a while)"
        python3 number-generator.py
    fi
}

task_1a() { ensure_data; title "Task 1a: split the file among 1-64 threads"; java/run.sh 1a; }
task_1b() { ensure_data; title "Task 1b: unsafe / lock / local counters";     java/run.sh 1b; }
task_1c() { ensure_data; title "Task 1c: reader + queue + 1-64 workers";      java/run.sh 1c; }
task_1()  { task_1a; task_1b; task_1c; }

# Task 2 on the emulated drive: mount, run 1/2/4 threads (re-plugged before each), unmount.
task_2_emulated() {
    ensure_data
    title "Task 2: emulated USB drive (25 MB/s)"
    java/sim-usb.sh up 25
    java/sim-usb.sh bench
    java/sim-usb.sh down
}

# Task 2 on a real drive: the user re-plugs it before every run, as the handout requires.
task_2_real() {
    local dir="$1"
    title "Task 2: real USB drive at $dir"
    for t in 1 2 4; do
        read -r -p "Eject, unplug and plug the drive back in, then press Enter for $t thread(s)... "
        [[ -d "$dir" ]] || { echo "not found: $dir (is the drive mounted?)" >&2; return 1; }
        java/run.sh 2 "$t" "$dir"
    done
}

# ---------------------------------------------------------------- menus

# Runs one menu action; a failure is reported instead of closing the CLI.
run() {
    "$@" || printf '\n%sfailed (exit code %s)%s\n' "$bold" "$?" "$reset"
    pause
}

real_usb() {
    read -r -e -p "Directory with part-*.txt on the drive (e.g. /Volumes/USB/parts): " dir
    task_2_real "$dir"
}

menu_task_2() {
    MENU_START=0 menu_select "Task 2 - where are the 8 files?" \
        "Emulated USB drive   no hardware needed, 25 MB/s" \
        "Real USB drive       re-plug before every run"
    case "$MENU_CHOICE" in
        0) run task_2_emulated ;;
        1) run real_usb ;;
    esac
}

cli() {
    local last=0
    while true; do
        MENU_START=$last menu_select "Lab 1 - Concurrency in practice" \
            "Task 1a   split the file among 1-64 threads" \
            "Task 1b   three ways to count: unsafe, lock, local" \
            "Task 1c   reader + bounded queue + 1-64 workers" \
            "Task 1    all of the above" \
            "Task 2    many files on slow storage" \
            "Quit"
        last=$MENU_CHOICE
        case "$MENU_CHOICE" in
            0) run task_1a ;;
            1) run task_1b ;;
            2) run task_1c ;;
            3) run task_1 ;;
            4) menu_task_2 ;;
            *) exit 0 ;;
        esac
    done
}

# ---------------------------------------------------------------- entry point

case "${1:-}" in
    -cli|--cli) cli ;;
    1a) task_1a ;;
    1b) task_1b ;;
    1c) task_1c ;;
    1)  task_1 ;;
    2)  if [[ -n "${2:-}" ]]; then task_2_real "$2"; else task_2_emulated; fi ;;
    *)  sed -n '2,13p' "$0" ;;
esac
