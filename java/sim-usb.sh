#!/usr/bin/env bash
# Task 2 without a USB stick (no sudo needed).
#
# parts/ is packed into a read-only disk image. java/usb-server.py serves that image over
# HTTP on localhost with ONE shared bandwidth cap, and `hdiutil attach` mounts it as a real
# volume. The kernel reads the volume through the capped server, so `Main 2` reads a slow
# device whose total speed does not grow with the number of threads, like one USB bus.
# Detaching and re-attaching the image empties its file cache, like unplugging the stick.
# Spotlight is kept off the volume (-nobrowse, read-only, .metadata_never_index).
#
#   java/sim-usb.sh up [MB/s]   build the image, start the server, mount (default 25 MB/s)
#   java/sim-usb.sh run <n>     "re-plug" (detach + attach), run Task 2 with n threads
#   java/sim-usb.sh bench       run 1, 2 and 4 threads, re-plugging before each
#   java/sim-usb.sh status      show what is active
#   java/sim-usb.sh down        unmount, stop the server (the image is kept; `clean` deletes it)
set -euo pipefail
cd "$(dirname "$0")/.."

dir="$PWD/.sim-usb"
image="$dir/usb.dmg"
mnt="$dir/mnt"
pidfile="$dir/server.pid"
port=8765
url="http://127.0.0.1:$port/usb.dmg"

server_running() { [[ -f "$pidfile" ]] && kill -0 "$(cat "$pidfile")" 2>/dev/null; }
mounted() { mount | grep -q " on $mnt "; }

detach() { if mounted; then hdiutil detach -quiet "$mnt" || hdiutil detach -quiet -force "$mnt"; fi; }

attach() {
    server_running || { echo "not up: run 'java/sim-usb.sh up' first" >&2; exit 1; }
    mkdir -p "$mnt"
    hdiutil attach -quiet -nobrowse -readonly -noverify -mountpoint "$mnt" "$url"
}

replug() { detach; attach; }

build_image() {
    [[ -f "$image" ]] && return
    [[ -d parts ]] || { javac -d java/out $(find java/src -name "*.java"); java -cp java/out Main split numbers.txt parts; }
    local src="$dir/src"
    rm -rf "$src" && mkdir -p "$src/parts"
    cp parts/part-*.txt "$src/parts/"
    touch "$src/.metadata_never_index"
    echo "building $image …"
    hdiutil create -quiet -srcfolder "$src" -format UDRO -fs HFS+ -volname SIMUSB -ov "$image"
    rm -rf "$src"
}

case "${1:-}" in
up)
    mbps="${2:-25}"
    mkdir -p "$dir"
    build_image
    detach
    if server_running; then kill "$(cat "$pidfile")"; sleep 0.5; fi
    nohup python3 java/usb-server.py "$image" "$port" "$mbps" >"$dir/server.log" 2>&1 &
    echo $! >"$pidfile"
    for _ in {1..20}; do nc -z 127.0.0.1 "$port" 2>/dev/null && break; sleep 0.25; done
    attach
    echo "mounted at $mnt/parts, capped at $mbps MB/s. Cold read of one file:"
    dd if="$mnt/parts/part-1.txt" of=/dev/null bs=1m 2>&1 | tail -1
    ;;
run)
    threads="${2:?usage: sim-usb.sh run <threads>}"
    replug
    java/run.sh 2 "$threads" "$mnt/parts"
    ;;
bench)
    for t in 1 2 4; do
        replug
        java/run.sh 2 "$t" "$mnt/parts"
    done
    ;;
status)
    server_running && echo "server: running (pid $(cat "$pidfile"), port $port)" || echo "server: stopped"
    mounted && echo "mounted: $mnt" || echo "mounted: no"
    [[ -f "$image" ]] && echo "image:   $image ($(du -h "$image" | cut -f1))" || echo "image:   none"
    ;;
down)
    detach
    if server_running; then kill "$(cat "$pidfile")"; fi
    rm -f "$pidfile"
    echo "down: unmounted and server stopped"
    ;;
clean)
    "$0" down
    rm -rf "$dir"
    echo "clean: removed $dir"
    ;;
*)
    sed -n '2,15p' "$0"
    ;;
esac
