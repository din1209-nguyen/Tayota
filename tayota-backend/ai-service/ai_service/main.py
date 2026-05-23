import os
import shutil
import uuid
from pathlib import Path

from fastapi import BackgroundTasks, FastAPI, File, Header, HTTPException, UploadFile
from redis import Redis
from redis.exceptions import RedisError

import rag
import vector_database
from ai_service.config import DOCUMENTS_DIR, REDIS_URL, SESSION_TTL_SECONDS
from ai_service.jobs import RedisJobStore
from ai_service.redis_state import RedisConversationStateManager
from ai_service.schemas import (
    ChatRequest,
    ChatResponse,
    DocumentJobQueued,
    DocumentJobStatus,
    HealthResponse,
    SessionResetResponse,
)


redis_client = Redis.from_url(REDIS_URL, decode_responses=True)
state_manager = RedisConversationStateManager(redis_client, ttl_seconds=SESSION_TTL_SECONDS)
job_store = RedisJobStore(redis_client)

rag.state_manager = state_manager

app = FastAPI(title="Car Chatbot AI Service", version="1.0.0")


@app.post("/api/v1/chat", response_model=ChatResponse)
def chat(
    payload: ChatRequest,
    x_user_id: str | None = Header(default=None, alias="X-User-Id"),
) -> ChatResponse:
    _user_id = x_user_id or payload.user_id

    try:
        result = rag.answer(payload.message, session_id=payload.session_id)
    except RedisError as exc:
        raise HTTPException(
            status_code=503,
            detail=f"Redis is unavailable; conversation state was not saved: {exc}",
        ) from exc

    return ChatResponse(
        answer=result["answer"],
        sources=result.get("sources", []),
        intent=result.get("intent", ""),
        stage=result.get("stage", ""),
        slots=result.get("slots", {}),
        session_id=result.get("session_id", payload.session_id),
    )


@app.post("/api/v1/documents", response_model=DocumentJobQueued)
def upload_document(
    background_tasks: BackgroundTasks,
    file: UploadFile = File(...),
    rebuild: bool = False,
) -> DocumentJobQueued:
    if not file.filename or Path(file.filename).suffix.lower() != ".pdf":
        raise HTTPException(status_code=400, detail="Only PDF uploads are supported.")

    try:
        redis_client.ping()
    except RedisError as exc:
        raise HTTPException(
            status_code=503,
            detail=f"Redis is unavailable; document job was not queued: {exc}",
        ) from exc

    DOCUMENTS_DIR.mkdir(parents=True, exist_ok=True)
    safe_name = Path(file.filename).name
    target_path = DOCUMENTS_DIR / safe_name
    if target_path.exists():
        target_path = DOCUMENTS_DIR / f"{target_path.stem}-{uuid.uuid4().hex[:8]}{target_path.suffix}"

    with target_path.open("wb") as out_file:
        shutil.copyfileobj(file.file, out_file)

    job_id = str(uuid.uuid4())
    job_store.set_status(job_id, "queued", message=f"Queued {target_path.name}")
    background_tasks.add_task(_run_ingest_job, job_id, target_path, rebuild)

    return DocumentJobQueued(job_id=job_id, status="queued")


@app.get("/api/v1/documents/jobs/{job_id}", response_model=DocumentJobStatus)
def get_document_job(job_id: str) -> DocumentJobStatus:
    try:
        status = job_store.get_status(job_id)
    except RedisError as exc:
        raise HTTPException(status_code=503, detail=f"Redis is unavailable: {exc}") from exc

    if status is None:
        raise HTTPException(status_code=404, detail="Document job not found.")
    return status


@app.post("/api/v1/sessions/{session_id}/reset", response_model=SessionResetResponse)
def reset_session(session_id: str) -> SessionResetResponse:
    try:
        state_manager.reset(session_id)
    except RedisError as exc:
        raise HTTPException(status_code=503, detail=f"Redis is unavailable: {exc}") from exc
    return SessionResetResponse(session_id=session_id, status="reset")


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    redis_status = "ok"
    qdrant_status = "ok"

    try:
        redis_client.ping()
    except RedisError:
        redis_status = "error"

    try:
        vector_database.get_client().get_collections()
    except Exception:
        qdrant_status = "error"

    llm_status = "configured" if os.getenv("GROQ_API_KEY") else "missing"
    overall = "ok" if redis_status == qdrant_status == "ok" and llm_status == "configured" else "degraded"

    return HealthResponse(
        status=overall,
        qdrant=qdrant_status,
        redis=redis_status,
        llm=llm_status,
    )


def _run_ingest_job(job_id: str, target_path: Path, rebuild: bool) -> None:
    try:
        job_store.set_status(job_id, "running", message="Indexing document")
        pdf_paths = [str(DOCUMENTS_DIR)] if rebuild else [str(target_path)]
        result = vector_database.ingest_documents(rebuild=rebuild, pdf_paths=pdf_paths)
        job_store.set_status(
            job_id,
            "success",
            message="Document indexed successfully",
            indexed_pages=int(result.get("indexed_pages", 0)),
            indexed_chunks=int(result.get("indexed_chunks", result.get("vectors_count", 0))),
        )
    except Exception as exc:
        job_store.set_status(job_id, "failed", message=str(exc))
