import json
from typing import Any

from redis import Redis

from ai_service.config import JOB_TTL_SECONDS
from ai_service.schemas import DocumentJobStatus


class RedisJobStore:
    def __init__(self, redis_client: Redis, ttl_seconds: int = JOB_TTL_SECONDS):
        self.redis = redis_client
        self.ttl_seconds = ttl_seconds

    def _key(self, job_id: str) -> str:
        return f"document_job:{job_id}"

    def set_status(
        self,
        job_id: str,
        status: str,
        message: str = "",
        indexed_pages: int = 0,
        indexed_chunks: int = 0,
    ) -> None:
        payload = {
            "job_id": job_id,
            "status": status,
            "message": message,
            "indexed_pages": indexed_pages,
            "indexed_chunks": indexed_chunks,
        }
        self.redis.setex(
            self._key(job_id),
            self.ttl_seconds,
            json.dumps(payload, ensure_ascii=False),
        )

    def get_status(self, job_id: str) -> DocumentJobStatus | None:
        raw = self.redis.get(self._key(job_id))
        if raw is None:
            return None
        if isinstance(raw, bytes):
            raw = raw.decode("utf-8")
        payload: dict[str, Any] = json.loads(raw)
        return DocumentJobStatus(**payload)
