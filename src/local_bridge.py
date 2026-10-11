#!/usr/bin/env python3
import json
import mimetypes
import email.policy
import os
import shutil
import subprocess
import threading
import uuid
from email.parser import BytesParser
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse
from pathlib import Path

ROOT = Path(__file__).resolve().parent
FRONTEND_DIR = ROOT / "frontend"
UPLOAD_DIR = ROOT / "uploads"
BACKUP_ROOT = ROOT / "backup-restore"
PROOF_DIR = ROOT / "validation-proof"
PROOF_FILE = PROOF_DIR / "validation-proof-report.txt"
JOBS = {}
JOBS_LOCK = threading.Lock()
LOCAL_ORIGINS = {"http://127.0.0.1:8000", "http://localhost:8000"}

UPLOAD_DIR.mkdir(exist_ok=True)
PROOF_DIR.mkdir(exist_ok=True)


def compile_java():
    subprocess.run(["javac", "Main.java"], cwd=str(ROOT), capture_output=True, text=True, check=False)


def run_java_cli(commands):
    compile_java()
    payload = "\n".join(str(item) for item in commands) + "\n"
    result = subprocess.run(
        ["java", "Main"],
        cwd=str(ROOT),
        input=payload,
        text=True,
        capture_output=True,
        check=False,
    )
    return {
        "returncode": result.returncode,
        "stdout": result.stdout,
        "stderr": result.stderr,
    }


def list_backups():
    if not BACKUP_ROOT.exists():
        return []
    entries = []
    for child in sorted(BACKUP_ROOT.iterdir(), key=lambda p: p.name.lower()):
        if child.is_dir():
            entries.append({
                "name": child.name,
                "path": str(child),
            })
    return entries


def ensure_proof_exists():
    if not PROOF_FILE.exists():
        run_java_cli(["23", "7"])


def update_job(job_id, **updates):
    with JOBS_LOCK:
        job = JOBS.get(job_id)
        if job is not None:
            job.update(updates)


def resolve_staged_file(value):
    try:
        file_path = Path(value).resolve(strict=True)
        file_path.relative_to(UPLOAD_DIR.resolve())
    except (TypeError, OSError, ValueError):
        return None

    return file_path if file_path.is_file() else None


def run_shred_job(job_id, file_path, passes):
    try:
        compilation = subprocess.run(
            ["javac", "Main.java"],
            cwd=str(ROOT),
            capture_output=True,
            text=True,
            check=False,
        )
        if compilation.returncode != 0:
            update_job(job_id, status="failed", error=compilation.stderr or compilation.stdout)
            return

        process = subprocess.Popen(
            ["java", "Main"],
            cwd=str(ROOT),
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1,
        )
        process.stdin.write(f"1\n{file_path}\n{passes}\n7\n")
        process.stdin.close()
        output_lines = []

        for line in process.stdout:
            if line.startswith("SHRED_PROGRESS:"):
                try:
                    _, current_pass, total_passes, bytes_written, file_size = line.strip().split(":")
                    current_pass = int(current_pass)
                    total_passes = int(total_passes)
                    bytes_written = int(bytes_written)
                    file_size = int(file_size)
                    if file_size == 0 or current_pass <= 0 or total_passes <= 0:
                        percent = 0
                    else:
                        pass_progress = current_pass - 1 + bytes_written / file_size
                        percent = round((pass_progress / total_passes) * 100)
                    update_job(
                        job_id,
                        percent=min(99, percent),
                        currentPass=current_pass,
                        totalPasses=total_passes,
                    )
                except (ValueError, ZeroDivisionError):
                    continue
            else:
                output_lines.append(line)

        returncode = process.wait()
        output = "".join(output_lines)
        success = returncode == 0 and "File shredded successfully." in output and not file_path.exists()
        update_job(
            job_id,
            status="complete" if success else "failed",
            percent=100 if success else JOBS[job_id]["percent"],
            stdout=output,
            error="" if success else output.strip() or "The Java shred operation did not complete.",
        )
    except Exception as exc:
        update_job(job_id, status="failed", error=str(exc))


class BridgeHandler(BaseHTTPRequestHandler):
    def has_trusted_origin(self):
        return self.headers.get("Origin") in LOCAL_ORIGINS

    def do_OPTIONS(self):
        origin = self.headers.get("Origin")
        if origin not in LOCAL_ORIGINS:
            self.send_response(403)
            self.end_headers()
            return

        self.send_response(200)
        self.send_header("Access-Control-Allow-Origin", origin)
        self.send_header("Access-Control-Allow-Methods", "POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.send_header("Vary", "Origin")
        self.end_headers()

    def send_json(self, status, payload):
        body = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def serve_static_file(self, relative_path):
        if relative_path in ("", "/"):
            relative_path = "index.html"
        if relative_path.startswith("/"):
            relative_path = relative_path[1:]

        target = (FRONTEND_DIR / relative_path).resolve()
        try:
            target.relative_to(FRONTEND_DIR.resolve())
        except ValueError:
            self.send_response(403)
            self.end_headers()
            return

        if not target.exists() or not target.is_file():
            self.send_response(404)
            self.end_headers()
            return

        mime_type, _ = mimetypes.guess_type(str(target))
        data = target.read_bytes()
        self.send_response(200)
        self.send_header("Content-Type", mime_type or "application/octet-stream")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        url = urlparse(self.path)
        path = url.path

        if path == "/api/health":
            self.send_json(200, {"status": "ok", "cwd": str(ROOT)})
            return

        if path == "/api/backups":
            self.send_json(200, {"backups": list_backups()})
            return

        if path.startswith("/api/jobs/"):
            job_id = path.rsplit("/", 1)[-1]
            with JOBS_LOCK:
                job = JOBS.get(job_id)
                snapshot = dict(job) if job is not None else None
            if snapshot is None:
                self.send_json(404, {"error": "job not found"})
            else:
                self.send_json(200, snapshot)
            return

        if path == "/api/proof":
            ensure_proof_exists()
            if not PROOF_FILE.exists():
                self.send_json(500, {"error": "no proof file generated"})
                return
            self.send_json(200, {"content": PROOF_FILE.read_text(encoding="utf-8")})
            return

        if path == "/api/proof-download":
            ensure_proof_exists()
            if not PROOF_FILE.exists():
                self.send_json(500, {"error": "no proof file generated"})
                return
            data = PROOF_FILE.read_bytes()
            self.send_response(200)
            self.send_header("Content-Type", "text/plain; charset=utf-8")
            self.send_header("Content-Disposition", "attachment; filename=validation-proof-report.txt")
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            self.wfile.write(data)
            return

        if path.startswith("/api/"):
            self.send_json(404, {"error": "endpoint not found"})
            return

        self.serve_static_file(path)

    def do_POST(self):
        url = urlparse(self.path)
        path = url.path

        if not self.has_trusted_origin():
            self.send_json(403, {"error": "Requests must come from this local app."})
            return

        if path == "/api/upload":
            try:
                content_type = self.headers.get('Content-Type', '')
                length = int(self.headers.get('Content-Length', '0'))
                body = self.rfile.read(length)
                message = BytesParser(policy=email.policy.default).parsebytes(
                    b'Content-Type: ' + content_type.encode('ascii')
                    + b'\r\nMIME-Version: 1.0\r\n\r\n' + body
                )
                uploaded = next((
                    part for part in message.iter_parts()
                    if part.get_param('name', header='content-disposition') == 'file'
                ), None)
                filename = uploaded.get_filename() if uploaded else None
                if not uploaded or not filename:
                    self.send_json(400, {"error": "no file provided"})
                    return

                safe_name = os.path.basename(filename.replace('\\', '/'))
                target = UPLOAD_DIR / f"{uuid.uuid4().hex}_{safe_name}"
                with open(target, 'wb') as handle:
                    handle.write(uploaded.get_payload(decode=True) or b'')

                self.send_json(200, {
                    "status": "staged locally",
                    "file": str(target),
                    "originalName": safe_name,
                })
                return
            except Exception as exc:
                self.send_json(500, {"error": str(exc)})
                return

        if path in ("/api/dry-run", "/api/shred"):
            try:
                length = int(self.headers.get('Content-Length', '0'))
                data = self.rfile.read(length)
                payload = json.loads(data.decode('utf-8')) if data else {}
                passes = int(payload.get("passes", 3))
            except (ValueError, json.JSONDecodeError) as exc:
                self.send_json(400, {"error": f"invalid request: {exc}"})
                return

            file_path = resolve_staged_file(payload.get("filePath", ""))
            if file_path is None:
                self.send_json(400, {"error": "Choose a file staged by this local app."})
                return
            if passes < 1 or passes > 7:
                self.send_json(400, {"error": "Overwrite passes must be between 1 and 7."})
                return

            if path == "/api/dry-run":
                result = run_java_cli(["3", str(file_path), str(passes), "7"])
                succeeded = result["returncode"] == 0 and "Dry run only:" in result["stdout"]
                self.send_json(200, {
                    "status": "complete" if succeeded else "failed",
                    "stdout": result["stdout"],
                    "stderr": result["stderr"],
                    "error": "The preview did not reach the dry-run result." if not succeeded else "",
                })
                return

            if payload.get("confirm") is not True:
                self.send_json(400, {"error": "Confirm that the staged copy will be deleted."})
                return

            job_id = uuid.uuid4().hex
            with JOBS_LOCK:
                JOBS[job_id] = {
                    "id": job_id,
                    "status": "running",
                    "percent": 0,
                    "currentPass": 0,
                    "totalPasses": passes,
                    "stdout": "",
                    "error": "",
                }
            threading.Thread(
                target=run_shred_job,
                args=(job_id, file_path, passes),
                daemon=True,
            ).start()
            self.send_json(202, {"jobId": job_id})
            return

        if path == "/api/backups":
            self.send_json(200, {"backups": list_backups()})
            return

        self.send_json(404, {"error": "endpoint not found"})


if __name__ == "__main__":
    compile_java()
    server = ThreadingHTTPServer(("127.0.0.1", 8000), BridgeHandler)
    print("Local shredder bridge running on http://127.0.0.1:8000")
    server.serve_forever()
