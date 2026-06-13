import os
import time
from contextlib import contextmanager
from typing import Dict


def env_flag(name: str, default: str = "false") -> bool:
    return os.getenv(name, default).lower() in {"1", "true", "yes", "on"}


def env_int(name: str, default: int) -> int:
    try:
        return int(os.getenv(name, str(default)))
    except (TypeError, ValueError):
        return default


def env_float(name: str, default: float) -> float:
    try:
        return float(os.getenv(name, str(default)))
    except (TypeError, ValueError):
        return default


class StageTimer:
    """Collect lightweight per-stage timings for one request."""

    def __init__(self, enabled: bool):
        self.enabled = enabled
        self.started_at = time.perf_counter()
        self.timings: Dict[str, float] = {}

    @contextmanager
    def track(self, name: str):
        if not self.enabled:
            yield
            return

        started = time.perf_counter()
        try:
            yield
        finally:
            elapsed_ms = (time.perf_counter() - started) * 1000
            self.timings[name] = self.timings.get(name, 0.0) + elapsed_ms

    def log(self, *, session_id: str, intent: str | None = None) -> None:
        if not self.enabled:
            return

        total_ms = (time.perf_counter() - self.started_at) * 1000
        parts = [
            "rag_timing",
            f"session={session_id}",
            f"total_ms={total_ms:.1f}",
        ]
        if intent:
            parts.append(f"intent={intent}")
        for key in sorted(self.timings):
            parts.append(f"{key}_ms={self.timings[key]:.1f}")
        print(" ".join(parts))
