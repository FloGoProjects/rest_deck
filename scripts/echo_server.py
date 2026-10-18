"""Test-Gegenstelle im LAN: antwortet auf jede Methode mit JSON-Echo.
Aufruf: python scripts/echo_server.py [port]   (Standard 8765)
/status/<code> antwortet mit diesem Statuscode, /slow/<sek> wartet vorher."""
import json, sys, time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

class Echo(BaseHTTPRequestHandler):
    def _handle(self):
        n = int(self.headers.get("Content-Length") or 0)
        body = self.rfile.read(n).decode("utf-8", "replace") if n else ""
        parts = self.path.strip("/").split("/")
        code = 200
        if len(parts) >= 2 and parts[0] == "status": code = int(parts[1])
        if len(parts) >= 2 and parts[0] == "slow": time.sleep(float(parts[1]))
        out = json.dumps({"method": self.command, "path": self.path, "headers": dict(self.headers), "body": body}, indent=2).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(out)))
        self.end_headers()
        if self.command != "HEAD": self.wfile.write(out)
    do_GET = do_POST = do_PUT = do_PATCH = do_DELETE = do_HEAD = _handle

port = int(sys.argv[1]) if len(sys.argv) > 1 else 8765
print(f"Echo-Server auf 0.0.0.0:{port}", flush=True)
ThreadingHTTPServer(("0.0.0.0", port), Echo).serve_forever()
