import os
import shutil
import uuid
from pathlib import Path
from typing import Any, Dict, Literal

from dotenv import load_dotenv
from fastapi import BackgroundTasks, FastAPI, File, HTTPException, UploadFile
from pydantic import BaseModel, Field

from conversation_state_manager import RedisStateError, state_manager
from rag import GROQ_API_KEY, LLM_PROVIDER, answer
from vector_database import get_collection_info, ingest_documents

load_dotenv()

DOCUMENTS_DIR = Path(os.getenv("DOCUMENTS_DIR", "documents")).resolve()
JOB_TTL_SECS = int(os.getenv("DOCUMENT_JOB_TTL_SECS", "86400"))

try:
    import redis
    from redis.exceptions import RedisError
except ImportError:  # pragma: no cover - only hit before dependencies are installed.
    redis = None

    class RedisError(Exception):
        pass


class Source(BaseModel):
    source: str | None = None
    page: int | None = None
    score: float | None = None


class ChatRequest(BaseModel):
    session_id: str = Field(..., min_length=1)
    user_id: str = Field(..., min_length=1)
    message: str = Field(..., min_length=1)


class ChatResponse(BaseModel):
    answer: str
    sources: list[Source]
    intent: str
    stage: str
    slots: Dict[str, Any]
    session_id: str


class DocumentJobResponse(BaseModel):
    job_id: str
    status: Literal["queued", "running", "success", "failed"]


class DocumentJobStatus(BaseModel):
    job_id: str
    status: Literal["queued", "running", "success", "failed"]
    message: str = ""
    indexed_pages: int = 0
    indexed_chunks: int = 0


class ResetSessionResponse(BaseModel):
    session_id: str
    status: Literal["reset"]


class HealthResponse(BaseModel):
    status: str
    qdrant: str
    redis: str
    llm: str


class JobStore:
    def __init__(self, redis_url: str | None = None):
        self.redis_url = redis_url or os.getenv("REDIS_URL", "redis://localhost:6379/0")
        self._client = None

    def _key(self, job_id: str) -> str:
        return f"ai-service:document-jobs:{job_id}"

    def _get_client(self):
        if redis is None:
            raise RedisStateError(
                "Redis dependency is not installed. Run `pip install -r requirements.txt`."
            )
        if self._client is None:
            self._client = redis.Redis.from_url(
                self.redis_url,
                decode_responses=True,
                socket_connect_timeout=2,
                socket_timeout=2,
            )
        return self._client

    def set(self, status: DocumentJobStatus) -> None:
        try:
            self._get_client().setex(
                self._key(status.job_id),
                JOB_TTL_SECS,
                status.model_dump_json(),
            )
        except RedisError as exc:
            raise RedisStateError(f"Cannot save document job to Redis: {exc}") from exc

    def get(self, job_id: str) -> DocumentJobStatus | None:
        try:
            payload = self._get_client().get(self._key(job_id))
        except RedisError as exc:
            raise RedisStateError(f"Cannot load document job from Redis: {exc}") from exc
        if not payload:
            return None
        return DocumentJobStatus.model_validate_json(payload)


job_store = JobStore()
app = FastAPI(title="Toyota RAG AI Service", version="1.0.0")


def _service_unavailable(detail: str) -> HTTPException:
    return HTTPException(status_code=503, detail=detail)


def _safe_filename(filename: str) -> str:
    name = Path(filename).name
    if not name.lower().endswith(".pdf"):
        raise HTTPException(status_code=400, detail="Only PDF uploads are supported.")
    return name


def _run_ingest_job(job_id: str, pdf_path: str, rebuild: bool) -> None:
    job_store.set(
        DocumentJobStatus(
            job_id=job_id,
            status="running",
            message="Indexing document.",
        )
    )
    try:
        ingest_paths = [str(DOCUMENTS_DIR)] if rebuild else [pdf_path]
        info = ingest_documents(rebuild=rebuild, pdf_paths=ingest_paths)
        indexed_chunks = int(info.get("vectors_count") or info.get("points_count") or 0)
        job_store.set(
            DocumentJobStatus(
                job_id=job_id,
                status="success",
                message="Document indexed successfully.",
                indexed_chunks=indexed_chunks,
            )
        )
    except Exception as exc:
        job_store.set(
            DocumentJobStatus(
                job_id=job_id,
                status="failed",
                message=str(exc),
            )
        )


@app.post("/api/v1/chat", response_model=ChatResponse)
def chat(request: ChatRequest) -> ChatResponse:
    try:
        result = answer(request.message, session_id=request.session_id)
    except RedisStateError as exc:
        raise _service_unavailable(str(exc)) from exc
    except RuntimeError as exc:
        raise _service_unavailable(str(exc)) from exc

    return ChatResponse(
        answer=result["answer"],
        sources=[Source.model_validate(source) for source in result.get("sources", [])],
        intent=result["intent"],
        stage=result["stage"],
        slots=result["slots"],
        session_id=result["session_id"],
    )


@app.post("/api/v1/documents", response_model=DocumentJobResponse)
def upload_document(
    background_tasks: BackgroundTasks,
    file: UploadFile = File(...),
    rebuild: bool = False,
) -> DocumentJobResponse:
    filename = _safe_filename(file.filename or "")
    DOCUMENTS_DIR.mkdir(parents=True, exist_ok=True)
    target = DOCUMENTS_DIR / filename

    with target.open("wb") as out_file:
        shutil.copyfileobj(file.file, out_file)

    job_id = str(uuid.uuid4())
    status = DocumentJobStatus(
        job_id=job_id,
        status="queued",
        message=f"Queued ingest for {filename}.",
    )
    try:
        job_store.set(status)
    except RedisStateError as exc:
        raise _service_unavailable(str(exc)) from exc

    background_tasks.add_task(_run_ingest_job, job_id, str(target), rebuild)
    return DocumentJobResponse(job_id=job_id, status="queued")


@app.get("/api/v1/documents/jobs/{job_id}", response_model=DocumentJobStatus)
def get_document_job(job_id: str) -> DocumentJobStatus:
    try:
        status = job_store.get(job_id)
    except RedisStateError as exc:
        raise _service_unavailable(str(exc)) from exc
    if status is None:
        raise HTTPException(status_code=404, detail="Document job not found.")
    return status


@app.post("/api/v1/sessions/{session_id}/reset", response_model=ResetSessionResponse)
def reset_session(session_id: str) -> ResetSessionResponse:
    try:
        state_manager.reset(session_id)
    except RedisStateError as exc:
        raise _service_unavailable(str(exc)) from exc
    return ResetSessionResponse(session_id=session_id, status="reset")


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    redis_status = "ok"
    qdrant_status = "ok"

    try:
        ping = getattr(state_manager, "ping", None)
        if ping:
            ping()
    except Exception as exc:
        redis_status = f"error: {exc}"

    try:
        get_collection_info()
    except Exception as exc:
        qdrant_status = f"error: {exc}"

    if LLM_PROVIDER == "groq":
        llm_status = "configured" if GROQ_API_KEY else "missing_groq_api_key"
    else:
        llm_status = f"configured:{LLM_PROVIDER}"

    status = "ok" if redis_status == "ok" and qdrant_status == "ok" else "degraded"
    return HealthResponse(
        status=status,
        qdrant=qdrant_status,
        redis=redis_status,
        llm=llm_status,
    )
