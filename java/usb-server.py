#!/usr/bin/env python3
"""Serves one disk image over HTTP with a single shared bandwidth cap (a "USB bus").

hdiutil attaches the image straight from this server, so the kernel reads the volume
through it: every reader, on every thread, shares the same MB/s, like one USB stick.

    python3 java/usb-server.py <image> <port> <MB/s>
"""
import os
import re
import sys
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

image, port, mbps = sys.argv[1], int(sys.argv[2]), float(sys.argv[3])
size = os.path.getsize(image)
bytes_per_second = mbps * 1_000_000
chunk = 64 * 1024


class Bus:
    """Hands out transfer slots in order; total throughput never exceeds bytes_per_second."""

    def __init__(self):
        self.lock = threading.Lock()
        self.next_free = time.monotonic()

    def take(self, n):
        with self.lock:
            now = time.monotonic()
            self.next_free = max(self.next_free, now) + n / bytes_per_second
            done = self.next_free
        delay = done - time.monotonic()
        if delay > 0:
            time.sleep(delay)


bus = Bus()


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, *args):
        pass

    def _range(self):
        m = re.match(r"bytes=(\d*)-(\d*)", self.headers.get("Range", ""))
        if not m:
            return 0, size - 1, False
        start = int(m.group(1)) if m.group(1) else size - int(m.group(2))
        end = int(m.group(2)) if m.group(1) and m.group(2) else size - 1
        return start, min(end, size - 1), True

    def _headers(self, start, end, partial):
        self.send_response(206 if partial else 200)
        self.send_header("Accept-Ranges", "bytes")
        self.send_header("Content-Type", "application/octet-stream")
        self.send_header("Content-Length", str(end - start + 1))
        if partial:
            self.send_header("Content-Range", f"bytes {start}-{end}/{size}")
        self.end_headers()

    def do_HEAD(self):
        self._headers(*self._range())

    def do_GET(self):
        start, end, partial = self._range()
        self._headers(start, end, partial)
        with open(image, "rb") as f:
            f.seek(start)
            left = end - start + 1
            while left > 0:
                data = f.read(min(chunk, left))
                if not data:
                    break
                bus.take(len(data))
                self.wfile.write(data)
                left -= len(data)


ThreadingHTTPServer.daemon_threads = True
ThreadingHTTPServer(("127.0.0.1", port), Handler).serve_forever()
