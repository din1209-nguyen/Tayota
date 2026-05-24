import tempfile
import uuid
from pathlib import Path
from typing import Any, Dict, Literal

from dotenv import load_dotenv
from fastapi import BackgroundTasks, FastAPI, File, Header, HTTPException, Query, UploadFile
from pydantic import BaseModel, Field

from conversation_state_manager import MongoStateError, state_manager
from mongo_storage import (
    MongoConnection,
    MongoDocumentJobStore,
    MongoDocumentStore,
    MongoStorageError,
)
from rag import GROQ_API_KEY, LLM_PROVIDER, answer
from vector_database import delete_document_chunks, get_collection_info, ingest_documents

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
    message: str = Field(..., min_length=1)


class ChatResponse(BaseModel):
    answer: str
    sources: list[Source]
    intent: str
    stage: str
    slots: Dict[str, Any]
    session_id: str


class ChatMessage(BaseModel):
    session_id: str
    user_id: str | None = None
    question: str
    answer: str
    intent: str
    stage: str
    slots_snapshot: Dict[str, Any]
    sources: list[Dict[str, Any]]
    model_used: str
    rule_triggered: str
    created_at: Any = None


class ChatMessagesResponse(BaseModel):
    session_id: str
    count: int
    messages: list[ChatMessage]


class UserSession(BaseModel):
    session_id: str
    user_id: str | None = None
    stage: str | None = None
    turn_count: int = 0
    last_intent: str | None = None
    filled_slots: Dict[str, Any]
    history_len: int = 0
    status: str = "active"
    created_at: Any = None
    updated_at: Any = None


class UserSessionsResponse(BaseModel):
    user_id: str
    count: int
    sessions: list[UserSession]


class DocumentItem(BaseModel):
    document_id: str
    filename: str
    status: str
    content_type: str | None = None
    size_bytes: int = 0
    sha256: str | None = None
    uploaded_by_user_id: str | None = None
    uploaded_at: Any = None
    updated_at: Any = None


class DocumentsResponse(BaseModel):
    count: int
    documents: list[DocumentItem]


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


class DeleteDocumentResponse(BaseModel):
    document_id: str
    filename: str | None = None
    status: Literal["deleted"]
    deleted_chunks: int = 0


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
DEFAULT_DOCUMENT_STATUSES = ("uploaded", "indexing", "indexed", "failed")


def _service_unavailable(detail: str) -> HTTPException:
    """Tạo lỗi 503 khi service phụ thuộc như MongoDB hoặc Qdrant không sẵn sàng."""
    return HTTPException(status_code=503, detail=detail)


def _safe_filename(filename: str) -> str:
    """Chuẩn hóa tên file upload và chỉ cho phép tài liệu PDF."""
    name = Path(filename).name
    if not name.lower().endswith(".pdf"):
        raise HTTPException(status_code=400, detail="Only PDF uploads are supported.")
    return name


def _has_admin_role(user_role: str | None) -> bool:
    """Kiểm tra header role có chứa quyền ADMIN hay không."""
    if not user_role:
        return False
    roles = {
        role.strip().upper().removeprefix("ROLE_")
        for role in user_role.split(",")
        if role.strip()
    }
    return "ADMIN" in roles


def _require_admin(user_role: str | None) -> None:
    """Chặn request không có quyền admin bằng lỗi HTTP 403."""
    if not _has_admin_role(user_role):
        raise HTTPException(status_code=403, detail="Admin role is required.")


def _qdrant_metadata_for_path(path: Path, document: Dict[str, Any]) -> Dict[str, Any]:
    """Tạo metadata gắn với file tạm để ingest chunk vào Qdrant."""
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
    """Chạy job nền để materialize PDF, ingest vào vector DB và cập nhật trạng thái."""
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
def chat(
    request: ChatRequest,
    session_id: str = Header(..., alias="X-AI-Session-Id", min_length=1),
    user_id: str | None = Header(default=None, alias="X-User-Id"),
) -> ChatResponse:
    """Xử lý một lượt chat và trả về câu trả lời cùng nguồn tham khảo."""
    try:
        result = answer(
            request.message,
            session_id=session_id,
            user_id=user_id,
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


@app.get("/api/v1/users/{user_id}/sessions", response_model=UserSessionsResponse)
def get_user_sessions(
    user_id: str,
    limit: int = Query(default=50, ge=1, le=200),
    offset: int = Query(default=0, ge=0),
) -> UserSessionsResponse:
    """Liệt kê các phiên hội thoại gần đây của một người dùng."""
    try:
        sessions = state_manager.list_user_sessions(
            user_id=user_id,
            limit=limit,
            offset=offset,
        )
    except MongoStateError as exc:
        raise _service_unavailable(str(exc)) from exc

    return UserSessionsResponse(
        user_id=user_id,
        count=len(sessions),
        sessions=[UserSession.model_validate(session) for session in sessions],
    )


@app.get("/api/v1/sessions/{session_id}/messages", response_model=ChatMessagesResponse)
def get_session_messages(
    session_id: str,
    limit: int = Query(default=50, ge=1, le=200),
    offset: int = Query(default=0, ge=0),
) -> ChatMessagesResponse:
    """Trả về lịch sử tin nhắn đã lưu của một session."""
    try:
        messages = state_manager.list_chat_messages(
            session_id=session_id,
            limit=limit,
            offset=offset,
        )
    except MongoStateError as exc:
        raise _service_unavailable(str(exc)) from exc

    return ChatMessagesResponse(
        session_id=session_id,
        count=len(messages),
        messages=[ChatMessage.model_validate(message) for message in messages],
    )


@app.post("/api/v1/documents", response_model=DocumentJobResponse)
def upload_document(
    background_tasks: BackgroundTasks,
    file: UploadFile = File(...),
    rebuild: bool = False,
    user_id: str | None = Header(default=None, alias="X-User-Id"),
    user_role: str | None = Header(default=None, alias="X-User-Role"),
) -> DocumentJobResponse:
    """Nhận file PDF từ admin và xếp lịch ingest tài liệu."""
    _require_admin(user_role)
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


@app.get("/api/v1/documents", response_model=DocumentsResponse)
def list_documents(
    status: list[str] | None = Query(default=None),
) -> DocumentsResponse:
    """Liệt kê tài liệu đã upload theo trạng thái được yêu cầu."""
    statuses = tuple(status) if status else DEFAULT_DOCUMENT_STATUSES
    try:
        documents = document_store.list_documents(statuses=statuses)
    except MongoStorageError as exc:
        raise _service_unavailable(str(exc)) from exc

    documents.sort(key=lambda item: str(item.get("uploaded_at") or ""), reverse=True)
    return DocumentsResponse(
        count=len(documents),
        documents=[DocumentItem.model_validate(document) for document in documents],
    )


@app.get("/api/v1/documents/jobs/{job_id}", response_model=DocumentJobStatus)
def get_document_job(job_id: str) -> DocumentJobStatus:
    """Tra cứu trạng thái hiện tại của một job ingest tài liệu."""
    try:
        status = job_store.get(job_id)
    except MongoStorageError as exc:
        raise _service_unavailable(str(exc)) from exc
    if status is None:
        raise HTTPException(status_code=404, detail="Document job not found.")
    return DocumentJobStatus.model_validate(status)


@app.delete("/api/v1/documents/{document_id}", response_model=DeleteDocumentResponse)
def delete_document(
    document_id: str,
    user_role: str | None = Header(default=None, alias="X-User-Role"),
) -> DeleteDocumentResponse:
    """Xóa metadata tài liệu, file GridFS và các chunk tương ứng trong Qdrant."""
    _require_admin(user_role)
    try:
        document = document_store.get_document(document_id)
    except MongoStorageError as exc:
        raise _service_unavailable(str(exc)) from exc
    if document is None:
        raise HTTPException(status_code=404, detail="Document not found.")

    try:
        deleted_chunks = delete_document_chunks(document_id)
    except RuntimeError as exc:
        raise _service_unavailable(str(exc)) from exc

    try:
        deleted_document = document_store.delete_document(document_id)
    except MongoStorageError as exc:
        raise _service_unavailable(str(exc)) from exc
    if deleted_document is None:
        raise HTTPException(status_code=404, detail="Document not found.")

    return DeleteDocumentResponse(
        document_id=document_id,
        filename=str(deleted_document.get("filename") or ""),
        status="deleted",
        deleted_chunks=deleted_chunks,
    )


@app.post("/api/v1/sessions/{session_id}/reset", response_model=ResetSessionResponse)
def reset_session(session_id: str) -> ResetSessionResponse:
    """Reset trạng thái hội thoại của một session."""
    try:
        state_manager.reset(session_id)
    except MongoStateError as exc:
        raise _service_unavailable(str(exc)) from exc
    return ResetSessionResponse(session_id=session_id, status="reset")


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    """Kiểm tra nhanh trạng thái MongoDB, Qdrant và cấu hình LLM."""
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
