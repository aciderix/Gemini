#!/usr/bin/env python3
"""HTTP bridge exposing Gemini CLI as a mobile-consumable backend."""

from __future__ import annotations

import json
import os
import shlex
import subprocess
from dataclasses import dataclass, field
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path
from typing import Any

HOST = os.getenv("GEMINI_BRIDGE_HOST", "0.0.0.0")
PORT = int(os.getenv("GEMINI_BRIDGE_PORT", "8765"))
CLI_PATH = Path(os.getenv("GEMINI_CLI_PATH", "./third_party/gemini-cli")).resolve()


@dataclass
class SessionState:
    token: str | None = None
    cwd: Path = field(default_factory=lambda: CLI_PATH)


SESSION = SessionState()


def json_response(handler: BaseHTTPRequestHandler, status: int, payload: dict[str, Any]) -> None:
    body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    handler.send_response(status)
    handler.send_header("Content-Type", "application/json; charset=utf-8")
    handler.send_header("Content-Length", str(len(body)))
    handler.end_headers()
    handler.wfile.write(body)


class Handler(BaseHTTPRequestHandler):
    def do_POST(self) -> None:  # noqa: N802
        content_length = int(self.headers.get("Content-Length", "0"))
        payload = json.loads(self.rfile.read(content_length) or "{}")

        routes = {
            "/v1/account/login": self._login,
            "/v1/account/status": self._status,
            "/v1/cli/commands": self._commands,
            "/v1/cli/execute": self._execute,
        }
        callback = routes.get(self.path)
        if callback is None:
            json_response(self, 404, {"error": f"unknown route: {self.path}"})
            return

        callback(payload)

    def _login(self, payload: dict[str, Any]) -> None:
        token = str(payload.get("token", "")).strip()
        if not token:
            json_response(self, 400, {"error": "missing token"})
            return
        SESSION.token = token
        json_response(self, 200, {"ok": True, "message": "token stored in bridge session"})

    def _status(self, _: dict[str, Any]) -> None:
        json_response(
            self,
            200,
            {
                "ok": True,
                "authenticated": SESSION.token is not None,
                "cwd": str(SESSION.cwd),
                "cli_path": str(CLI_PATH),
            },
        )

    def _commands(self, _: dict[str, Any]) -> None:
        commands = [
            "/auth", "/login", "/logout", "/status", "/help", "/model", "/models", "/config", "/profile", "/workspace",
            "/pwd", "/ls", "/cd", "/cat", "/edit", "/write", "/mkdir", "/rm", "/mv", "/cp",
            "/find", "/search", "/grep", "/run", "/shell", "/terminal", "/exec", "/python", "/npm", "/git",
            "/branch", "/commit", "/diff", "/test", "/lint", "/format", "/plan", "/apply", "/undo", "/redo",
        ]
        json_response(self, 200, {"ok": True, "commands": commands, "count": len(commands)})

    def _execute(self, payload: dict[str, Any]) -> None:
        user_input = str(payload.get("input", "")).strip()
        requested_cwd = str(payload.get("cwd", "")).strip()
        if not user_input:
            json_response(self, 400, {"error": "missing input"})
            return

        if requested_cwd:
            SESSION.cwd = Path(requested_cwd).expanduser().resolve()

        # Direct shell execution for explicit /shell or /exec prefix.
        if user_input.startswith("/shell ") or user_input.startswith("/exec "):
            shell_cmd = user_input.split(" ", 1)[1]
            result = subprocess.run(
                shell_cmd,
                shell=True,
                cwd=SESSION.cwd,
                capture_output=True,
                text=True,
                timeout=180,
                check=False,
            )
            json_response(
                self,
                200 if result.returncode == 0 else 500,
                {
                    "ok": result.returncode == 0,
                    "mode": "shell",
                    "command": shell_cmd,
                    "returncode": result.returncode,
                    "stdout": result.stdout,
                    "stderr": result.stderr,
                },
            )
            return

        args = ["npm", "run", "gemini", "--", user_input]
        env = os.environ.copy()
        if SESSION.token:
            env["GEMINI_API_TOKEN"] = SESSION.token

        try:
            completed = subprocess.run(
                args,
                cwd=SESSION.cwd,
                env=env,
                capture_output=True,
                text=True,
                check=False,
                timeout=300,
            )
        except FileNotFoundError:
            json_response(self, 500, {"error": "npm not found on bridge host"})
            return
        except subprocess.TimeoutExpired:
            json_response(self, 504, {"error": "gemini command timeout"})
            return

        json_response(
            self,
            200 if completed.returncode == 0 else 500,
            {
                "ok": completed.returncode == 0,
                "mode": "gemini-cli",
                "cwd": str(SESSION.cwd),
                "argv": " ".join(shlex.quote(a) for a in args),
                "returncode": completed.returncode,
                "stdout": completed.stdout,
                "stderr": completed.stderr,
            },
        )


if __name__ == "__main__":
    print(f"Gemini CLI bridge listening on {HOST}:{PORT} using {CLI_PATH}")
    HTTPServer((HOST, PORT), Handler).serve_forever()
