import tempfile
import uuid
from pathlib import Path
from typing import Any, Dict, Literal

from dotenv import load_dotenv
from fastapi import BackgroundTasks, FastAPI, File, Form, HTTPException, UploadFile
from pydantic import BaseModel, Field

from conversation_state_manager import MongoStateError, state_manager
from mongo_storage import (
    MongoConnection,
    MongoDocumentJobStore,
    MongoDocumentStore,
    MongoStorageError,
)
from rag import GROQ_API_KEY, LLM_PROVIDER, answer
from vector_database import get_collection_info, ingest_documents

load_dotenv()


class Source(BaseModel):
    source: str | None = None
    page: int | None = None
    score: float | None = None
    chunk_id: str | None = None
    chunk_index: int | None = None
    document_id: str | None = None
    gridfs_file_id: str | None = None


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
    document_id: str | None = None
    indexed_pages: int = 0
    indexed_chunks: int = 0


class ResetSessionResponse(BaseModel):
    session_id: str
    status: Literal["reset"]


class HealthResponse(BaseModel):
    status: str
    qdrant: str
    mongo: str
    redis: str = "unused"
    llm: str


mongo_connection = MongoConnection()
document_store = MongoDocumentStore(mongo_connection)
job_store = MongoDocumentJobStore(mongo_connection)
app = FastAPI(title="Toyota RAG AI Service", version="1.0.0")


def _service_unavailable(detail: str) -> HTTPException:
    return HTTPException(status_code=503, detail=detail)


def _safe_filename(filename: str) -> str:
    name = Path(filename).name
    if not name.lower().endswith(".pdf"):
        raise HTTPException(status_code=400, detail="Only PDF uploads are supported.")
    return name


def _qdrant_metadata_for_path(path: Path, document: Dict[str, Any]) -> Dict[str, Any]:
    document_id = document.get("document_id")
    gridfs_file_id = str(document.get("gridfs_file_id"))
    return {
        str(path.resolve()): {
            "document_id": document_id,
            "gridfs_file_id": gridfs_file_id,
            "source_id": f"mongo-{document_id}",
            "source_key": f"gridfs/{gridfs_file_id}",
            "source_path": f"gridfs://{gridfs_file_id}",
        }
    }


def _run_ingest_job(job_id: str, document_id: str, rebuild: bool) -> None:
    job_store.set(
        DocumentJobStatus(
            job_id=job_id,
            status="running",
            message="Indexing document.",
            document_id=document_id,
        )
    )
    try:
        document_store.update_status(document_id, "indexing")
        with tempfile.TemporaryDirectory() as tmp_dir:
            pdf_metadata_by_path: Dict[str, Any] = {}
            if rebuild:
                documents = document_store.list_documents()
                paths = []
                for document in documents:
                    path = document_store.materialize_pdf(
                        str(document["document_id"]),
                        tmp_dir,
                    )
                    paths.append(path)
                    pdf_metadata_by_path.update(_qdrant_metadata_for_path(path, document))
            else:
                document = document_store.get_document(document_id)
                if not document:
                    raise MongoStorageError(f"Document '{document_id}' was not found.")
                path = document_store.materialize_pdf(document_id, tmp_dir)
                paths = [path]
                pdf_metadata_by_path.update(_qdrant_metadata_for_path(path, document))

            info = ingest_documents(
                rebuild=rebuild,
                pdf_paths=[str(path) for path in paths],
                pdf_metadata_by_path=pdf_metadata_by_path,
            )
        indexed_chunks = int(info.get("vectors_count") or info.get("points_count") or 0)
        document_store.update_status(document_id, "indexed")
        job_store.set(
            DocumentJobStatus(
                job_id=job_id,
                status="success",
                message="Document indexed successfully.",
                document_id=document_id,
                indexed_chunks=indexed_chunks,
            )
        )
    except Exception as exc:
        try:
            document_store.update_status(document_id, "failed")
        except Exception:
            pass
        job_store.set(
            DocumentJobStatus(
                job_id=job_id,
                status="failed",
                message=str(exc),
                document_id=document_id,
            )
        )


@app.post("/api/v1/chat", response_model=ChatResponse)
def chat(request: ChatRequest) -> ChatResponse:
    try:
        result = answer(
            request.message,
            session_id=request.session_id,
            user_id=request.user_id,
        )
    except MongoStateError as exc:
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
    user_id: str | None = Form(default=None),
) -> DocumentJobResponse:
    filename = _safe_filename(file.filename or "")

    job_id = str(uuid.uuid4())
    try:
        document = document_store.save_pdf(
            filename=filename,
            content_type=file.content_type,
            file_obj=file.file,
            uploaded_by_user_id=user_id,
        )
        status = DocumentJobStatus(
            job_id=job_id,
            status="queued",
            message=f"Queued ingest for {filename}.",
            document_id=str(document["document_id"]),
        )
        job_store.set(status)
    except MongoStorageError as exc:
        raise _service_unavailable(str(exc)) from exc

    background_tasks.add_task(_run_ingest_job, job_id, str(document["document_id"]), rebuild)
    return DocumentJobResponse(job_id=job_id, status="queued")


@app.get("/api/v1/documents/jobs/{job_id}", response_model=DocumentJobStatus)
def get_document_job(job_id: str) -> DocumentJobStatus:
    try:
        status = job_store.get(job_id)
    except MongoStorageError as exc:
        raise _service_unavailable(str(exc)) from exc
    if status is None:
        raise HTTPException(status_code=404, detail="Document job not found.")
    return DocumentJobStatus.model_validate(status)


@app.post("/api/v1/sessions/{session_id}/reset", response_model=ResetSessionResponse)
def reset_session(session_id: str) -> ResetSessionResponse:
    try:
        state_manager.reset(session_id)
    except MongoStateError as exc:
        raise _service_unavailable(str(exc)) from exc
    return ResetSessionResponse(session_id=session_id, status="reset")


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    mongo_status = "ok"
    qdrant_status = "ok"

    try:
        mongo_connection.ping()
    except Exception as exc:
        mongo_status = f"error: {exc}"

    try:
        get_collection_info()
    except Exception as exc:
        qdrant_status = f"error: {exc}"

    if LLM_PROVIDER == "groq":
        llm_status = "configured" if GROQ_API_KEY else "missing_groq_api_key"
    else:
        llm_status = f"configured:{LLM_PROVIDER}"

    status = "ok" if mongo_status == "ok" and qdrant_status == "ok" else "degraded"
    return HealthResponse(
        status=status,
        qdrant=qdrant_status,
        mongo=mongo_status,
        llm=llm_status,
    )
