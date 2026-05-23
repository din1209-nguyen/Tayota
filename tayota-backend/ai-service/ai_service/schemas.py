from typing import Any, Literal

from pydantic import BaseModel, Field


class ChatRequest(BaseModel):
    session_id: str = Field(..., min_length=1)
    message: str = Field(..., min_length=1)
    user_id: str | None = Field(default=None, min_length=1)


class Source(BaseModel):
    source: str | None = None
    page: int | None = None
    score: float | None = None


class ChatResponse(BaseModel):
    answer: str
    sources: list[Source]
    intent: str
    stage: str
    slots: dict[str, Any]
    session_id: str


class DocumentJobQueued(BaseModel):
    job_id: str
    status: Literal["queued"]


class DocumentJobStatus(BaseModel):
    job_id: str
    status: Literal["queued", "running", "success", "failed"]
    message: str = ""
    indexed_pages: int = 0
    indexed_chunks: int = 0


class SessionResetResponse(BaseModel):
    session_id: str
    status: Literal["reset"]


class HealthResponse(BaseModel):
    status: str
    qdrant: str
    redis: str
    llm: str
