import argparse
import os
import sys
import uuid
from collections.abc import Callable
from typing import Any, Dict, List, TypeVar

from dotenv import load_dotenv
from qdrant_client import QdrantClient
from qdrant_client.http.exceptions import ResponseHandlingException
from qdrant_client.models import (
    Distance,
    FieldCondition,
    Filter,
    FilterSelector,
    MatchAny,
    MatchValue,
    PointStruct,
    VectorParams,
)


load_dotenv()

QDRANT_URL = os.getenv("QDRANT_URL", "http://localhost:6333")
QDRANT_API_KEY = os.getenv("QDRANT_API_KEY", None)
COLLECTION = os.getenv("COLLECTION", "atbm_httt")
EMBED_DIM = 384
BATCH_SIZE = 100
T = TypeVar("T")
_client: QdrantClient | None = None

DOCUMENT_CATEGORY_SUMMARY = "summary"
DOCUMENT_CATEGORY_BASIC_ADVICE = "basic_advice"
DOCUMENT_CATEGORY_HILUX = "hilux"
DOCUMENT_CATEGORY_SEDAN = "sedan"
DOCUMENT_CATEGORY_SUV = "suv"
DOCUMENT_CATEGORY_WIGO = "wigo"
DOCUMENT_CATEGORY_MPV = "mpv"


def infer_document_category(source: str | None) -> str | None:
    """Suy ra category ổn định từ tên/source tài liệu khi chưa có metadata rõ ràng."""
    if not source:
        return None
    normalized = source.casefold()
    if normalized == DOCUMENT_CATEGORY_SUMMARY or "summary" in normalized:
        return DOCUMENT_CATEGORY_SUMMARY
    if "tu van co ban" in normalized or "tư vấn cơ bản" in normalized:
        return DOCUMENT_CATEGORY_BASIC_ADVICE
    if "hilux" in normalized or "ban tai" in normalized or "bán tải" in normalized:
        return DOCUMENT_CATEGORY_HILUX
    if "sedan" in normalized or any(model in normalized for model in ("vios", "camry", "altis")):
        return DOCUMENT_CATEGORY_SEDAN
    if "suv" in normalized or any(
        model in normalized
        for model in ("fortuner", "land cruiser", "prado", "yaris cross", "corolla cross", "raize")
    ):
        return DOCUMENT_CATEGORY_SUV
    if "wigo" in normalized or "hatchback" in normalized:
        return DOCUMENT_CATEGORY_WIGO
    if "da dung" in normalized or "đa dụng" in normalized or any(
        model in normalized for model in ("mpv", "avanza", "veloz", "innova", "alphard")
    ):
        return DOCUMENT_CATEGORY_MPV
    return None


def _qdrant_hint() -> str:
    """Tạo hướng dẫn khắc phục khi không kết nối được Qdrant."""
    return (
        f"Khong ket noi duoc Qdrant tai {QDRANT_URL}.\n"
        "Hay khoi dong Qdrant truoc khi ingest/search:\n"
        "  docker compose up -d\n"
        "Sau do kiem tra dashboard: http://localhost:6333/dashboard"
    )


def _qdrant_call(action: str, operation: Callable[[], T]) -> T:
    """Bọc lời gọi Qdrant để chuyển lỗi kết nối thành thông báo dễ hiểu."""
    try:
        return operation()
    except ResponseHandlingException as exc:
        raise RuntimeError(f"{_qdrant_hint()}\nTac vu loi: {action}") from exc


def get_client() -> QdrantClient:
    """Khởi tạo QdrantClient từ cấu hình môi trường."""
    global _client
    if _client is None:
        _client = QdrantClient(url=QDRANT_URL, api_key=QDRANT_API_KEY)
    return _client


def qdrant_point_id(chunk_id: str) -> str:
    """Return a deterministic UUID point id from the stable chunk id."""
    return str(uuid.uuid5(uuid.NAMESPACE_URL, chunk_id))


def create_collection(recreate: bool = False) -> bool:
    """Create the Qdrant collection, optionally deleting the old one first."""
    client = get_client()
    collections = _qdrant_call("get collections", client.get_collections)
    existing = [c.name for c in collections.collections]
    collection_created = False

    if COLLECTION in existing:
        if recreate:
            _qdrant_call(
                f"delete collection '{COLLECTION}'",
                lambda: client.delete_collection(COLLECTION),
            )
            print(f"[INFO] Da xoa collection cu: {COLLECTION}")
        else:
            print(f"[INFO] Collection '{COLLECTION}' da ton tai, bo qua tao moi.")
            return False

    _qdrant_call(
        f"create collection '{COLLECTION}'",
        lambda: client.create_collection(
            collection_name=COLLECTION,
            vectors_config=VectorParams(size=EMBED_DIM, distance=Distance.COSINE),
        ),
    )
    print(f"[INFO] Da tao collection '{COLLECTION}' (dim={EMBED_DIM}, cosine)")
    collection_created = True
    return collection_created


def _chunk_payload(chunk: Dict[str, Any]) -> Dict[str, Any]:
    """Chuyển chunk nội bộ thành payload lưu trong Qdrant."""
    metadata = chunk.get("metadata", {})
    source = metadata.get("source")
    document_category = metadata.get("document_category") or infer_document_category(
        f"{source or ''}\n{chunk.get('content') or ''}"
    )
    return {
        "chunk_id": chunk["chunk_id"],
        "content": chunk["content"],
        "source": source,
        "source_id": metadata.get("source_id"),
        "source_path": metadata.get("source_path"),
        "document_category": document_category,
        "page": metadata.get("page"),
        "total_pages": metadata.get("total_pages"),
        "chunk_index": metadata.get("chunk_index"),
        "char_start": metadata.get("char_start"),
        "char_end": metadata.get("char_end"),
        "content_hash": metadata.get("content_hash"),
        "document_id": metadata.get("document_id"),
        "gridfs_file_id": metadata.get("gridfs_file_id"),
        "document_tags": metadata.get("document_tags", []),
        "mentioned_models": metadata.get("mentioned_models", []),
        "label_source": metadata.get("label_source"),
        "label_confidence": metadata.get("label_confidence"),
    }


def upsert_chunks(chunks: List[Dict[str, Any]]) -> None:
    """Upsert chunks into Qdrant using deterministic point ids."""
    client = get_client()
    total = 0

    for i in range(0, len(chunks), BATCH_SIZE):
        batch = chunks[i : i + BATCH_SIZE]
        points = [
            PointStruct(
                id=qdrant_point_id(c["chunk_id"]),
                vector=c["embedding"],
                payload=_chunk_payload(c),
            )
            for c in batch
        ]
        _qdrant_call(
            f"upsert {len(points)} points",
            lambda: client.upsert(collection_name=COLLECTION, points=points),
        )
        total += len(points)
        print(f"   Upserted {total}/{len(chunks)} points")

    print(f"[INFO] Hoan tat upsert {total} diem vao '{COLLECTION}'")


def search(
    query_vector: List[float],
    top_k: int = 5,
    score_threshold: float = 0.4,
    source_names: List[str] | None = None,
    document_categories: List[str] | None = None,
    document_tags: List[str] | None = None,
    mentioned_models: List[str] | None = None,
) -> List[Dict[str, Any]]:
    """Tìm các chunk gần nhất trong Qdrant theo vector truy vấn."""
    client = get_client()
    query_filter = _source_filter(
        source_names,
        document_categories,
        document_tags=document_tags,
        mentioned_models=mentioned_models,
    )

    if hasattr(client, "query_points"):
        results = _qdrant_call(
            "query points",
            lambda: client.query_points(
                collection_name=COLLECTION,
                query=query_vector,
                query_filter=query_filter,
                limit=top_k,
                score_threshold=score_threshold,
                with_payload=True,
            ).points,
        )
    else:
        results = _qdrant_call(
            "search points",
            lambda: client.search(
                collection_name=COLLECTION,
                query_vector=query_vector,
                query_filter=query_filter,
                limit=top_k,
                score_threshold=score_threshold,
                with_payload=True,
            ),
        )

    return [
        _point_payload_result(r.payload, score=r.score)
        for r in results
    ]


def search_neighbor_chunks(
    *,
    source_id: str | None,
    page: int | None,
    chunk_index: int | None,
    window: int = 1,
) -> List[Dict[str, Any]]:
    """Lấy các chunk liền kề cùng nguồn/trang để bổ sung ngữ cảnh."""
    if not source_id or page is None or chunk_index is None or chunk_index < 0:
        return []

    neighbor_indices = [
        idx
        for idx in range(chunk_index - window, chunk_index + window + 1)
        if idx >= 0 and idx != chunk_index
    ]
    if not neighbor_indices:
        return []

    client = get_client()
    query_filter = Filter(
        must=[
            FieldCondition(key="source_id", match=MatchValue(value=source_id)),
            FieldCondition(key="page", match=MatchValue(value=page)),
            FieldCondition(
                key="chunk_index",
                match=MatchAny(any=neighbor_indices),
            ),
        ]
    )
    points, _ = _qdrant_call(
        "scroll neighbor chunks",
        lambda: client.scroll(
            collection_name=COLLECTION,
            scroll_filter=query_filter,
            limit=len(neighbor_indices) + 2,
            with_payload=True,
            with_vectors=False,
        ),
    )
    neighbors = [_point_payload_result(p.payload, score=0.0) for p in points]
    neighbors.sort(key=lambda item: item.get("chunk_index") or 0)
    return neighbors


def scroll_chunks(
    *,
    source_names: List[str] | None = None,
    document_categories: List[str] | None = None,
    document_tags: List[str] | None = None,
    mentioned_models: List[str] | None = None,
    limit: int = 1000,
) -> List[Dict[str, Any]]:
    """Scroll chunk từ Qdrant, tùy chọn lọc theo danh sách source."""
    client = get_client()
    points, _ = _qdrant_call(
        "scroll chunks",
        lambda: client.scroll(
            collection_name=COLLECTION,
            scroll_filter=_source_filter(
                source_names,
                document_categories,
                document_tags=document_tags,
                mentioned_models=mentioned_models,
            ),
            limit=limit,
            with_payload=True,
            with_vectors=False,
        ),
    )
    return [_point_payload_result(p.payload, score=0.0) for p in points]


def _source_filter(
    source_names: List[str] | None,
    document_categories: List[str] | None = None,
    *,
    document_tags: List[str] | None = None,
    mentioned_models: List[str] | None = None,
) -> Filter | None:
    """Tạo filter Qdrant theo category ổn định, kèm fallback source cũ nếu có."""
    source_names = [source for source in (source_names or []) if source]
    document_categories = [
        category for category in (document_categories or []) if category
    ]
    document_tags = [tag for tag in (document_tags or []) if tag]
    mentioned_models = [model for model in (mentioned_models or []) if model]
    if (
        not source_names
        and not document_categories
        and not document_tags
        and not mentioned_models
    ):
        return None

    conditions = []
    if document_categories:
        conditions.append(
            FieldCondition(
                key="document_category",
                match=MatchAny(any=document_categories),
            )
        )
    if source_names:
        conditions.append(
            FieldCondition(
                key="source",
                match=MatchAny(any=source_names),
            )
        )
    if document_tags:
        conditions.append(
            FieldCondition(
                key="document_tags",
                match=MatchAny(any=document_tags),
            )
        )
    if mentioned_models:
        conditions.append(
            FieldCondition(
                key="mentioned_models",
                match=MatchAny(any=mentioned_models),
            )
        )

    if len(conditions) == 1:
        return Filter(must=conditions)
    return Filter(should=conditions)


def _point_payload_result(payload: Dict[str, Any], score: float = 1.0) -> Dict[str, Any]:
    """Chuẩn hóa payload Qdrant thành dict retrieved chunk dùng trong RAG."""
    return {
        "score": score,
        "content": payload["content"],
        "page": payload["page"],
        "source": payload["source"],
        "source_id": payload.get("source_id"),
        "document_category": payload.get("document_category")
        or infer_document_category(
            f"{payload.get('source') or ''}\n{payload.get('content') or ''}"
        ),
        "chunk_id": payload.get("chunk_id"),
        "chunk_index": payload.get("chunk_index"),
        "char_start": payload.get("char_start"),
        "char_end": payload.get("char_end"),
        "document_id": payload.get("document_id"),
        "gridfs_file_id": payload.get("gridfs_file_id"),
        "document_tags": payload.get("document_tags") or [],
        "mentioned_models": payload.get("mentioned_models") or [],
        "label_source": payload.get("label_source"),
        "label_confidence": payload.get("label_confidence"),
    }


def get_collection_info() -> Dict[str, Any]:
    """Trả về thông tin cơ bản về collection Qdrant hiện tại."""
    client = get_client()
    result = _qdrant_call(
        f"count collection '{COLLECTION}'",
        lambda: client.count(collection_name=COLLECTION),
    )
    return {
        "vectors_count": result.count,
        "status": "ok",
    }


def collection_point_count() -> int:
    """Đếm số point/vector hiện có trong collection."""
    client = get_client()
    return _qdrant_call(
        f"count collection '{COLLECTION}'",
        lambda: client.count(collection_name=COLLECTION),
    ).count


def _document_filter(document_id: str) -> Filter:
    """Tạo filter Qdrant để chọn chunk theo document_id."""
    return Filter(
        must=[
            FieldCondition(
                key="document_id",
                match=MatchValue(value=document_id),
            )
        ]
    )


def delete_document_chunks(document_id: str) -> int:
    """Xóa toàn bộ chunk của một document khỏi Qdrant và cập nhật summary."""
    client = get_client()
    query_filter = _document_filter(document_id)
    count = _qdrant_call(
        f"count chunks for document '{document_id}'",
        lambda: client.count(
            collection_name=COLLECTION,
            count_filter=query_filter,
            exact=True,
        ),
    ).count
    _qdrant_call(
        f"delete chunks for document '{document_id}'",
        lambda: client.delete(
            collection_name=COLLECTION,
            points_selector=FilterSelector(filter=query_filter),
            wait=True,
        ),
    )
    try:
        refresh_summary_chunk()
    except Exception as exc:
        print(f"[WARN] Khong cap nhat duoc summary chunk sau khi xoa document: {exc}")
    return count


def refresh_summary_chunk() -> None:
    """Tạo lại summary chunk dựa trên các chunk tài liệu hiện có."""
    chunks = [
        chunk
        for chunk in scroll_chunks(limit=10000)
        if chunk.get("source") != "summary"
    ]
    upsert_summary_chunk(_car_names_from_chunks(chunks))


def upsert_summary_chunk(car_names: List[str]) -> None:
    """Upsert one summary chunk for listing all available cars."""
    from embed import embed_texts

    unique_names = sorted({name for name in car_names if name})
    content = "Danh sach tat ca cac xe Toyota hien co: " + ", ".join(unique_names)
    vector = embed_texts([content])[0]
    chunk_id = "summary_all_cars"
    client = get_client()

    _qdrant_call(
        "upsert summary chunk",
        lambda: client.upsert(
            collection_name=COLLECTION,
            points=[
                PointStruct(
                    id=qdrant_point_id(chunk_id),
                    vector=vector,
                    payload={
                        "chunk_id": chunk_id,
                        "content": content,
                        "source": "summary",
                        "source_id": "summary",
                        "source_path": None,
                        "document_category": DOCUMENT_CATEGORY_SUMMARY,
                        "page": 0,
                        "total_pages": 0,
                        "chunk_index": -1,
                        "char_start": 0,
                        "char_end": len(content),
                        "content_hash": None,
                        "document_tags": [],
                        "mentioned_models": unique_names,
                        "label_source": "rule",
                        "label_confidence": 1.0,
                    },
                )
            ],
        ),
    )
    print(f"[INFO] Da upsert summary chunk: {content}")


def _car_names_from_chunks(chunks: List[Dict[str, Any]]) -> List[str]:
    """Suy ra danh sách tên xe/tài liệu từ source của các chunk."""
    names = []
    for chunk in chunks:
        source = chunk.get("metadata", {}).get("source") or chunk.get("source")
        if source:
            names.append(source.removesuffix(".pdf"))
    return sorted(set(names))


def ingest_documents(
    rebuild: bool = False,
    pdf_paths: List[str] | None = None,
    glob_pattern: str | None = None,
    pdf_metadata_by_path: Dict[str, Any] | None = None,
) -> Dict[str, Any]:
    """
    Ingest PDFs into Qdrant.

    rebuild=True is a clean rebuild: recreate collection, read every PDF from
    disk, chunk, embed, upsert, then create the summary chunk. The registry is
    only updated as a cache and is never used to decide rebuild input data.
    """
    from chunking import chunk_documents
    from data_processing.extract_pdf import (
        extract_all_pdfs,
        extract_multiple_pdfs,
        mark_documents_processed,
    )
    from document_labeler import apply_document_labels
    from embed import embed_chunks

    if rebuild:
        print("[INFO] Rebuild sach: recreate collection va doc toan bo PDF.")
        create_collection(recreate=True)
        docs = extract_all_pdfs(
            pdf_paths=pdf_paths,
            glob_pattern=glob_pattern,
            pdf_metadata_by_path=pdf_metadata_by_path,
        )
    else:
        print("[INFO] Ingest incremental: chi doc PDF moi/da thay doi theo cache.")
        collection_created = create_collection(recreate=False)
        if collection_created or collection_point_count() == 0:
            print("[INFO] Collection rong, bo qua registry va doc toan bo PDF.")
            docs = extract_all_pdfs(
                pdf_paths=pdf_paths,
                glob_pattern=glob_pattern,
                pdf_metadata_by_path=pdf_metadata_by_path,
            )
        else:
            docs = extract_multiple_pdfs(
                pdf_paths=pdf_paths,
                glob_pattern=glob_pattern,
                skip_processed=True,
                pdf_metadata_by_path=pdf_metadata_by_path,
            )

    if not docs:
        print("[INFO] Khong co tai lieu de ingest.")
        return get_collection_info()

    docs = apply_document_labels(docs)
    chunks = chunk_documents(docs)
    if not chunks:
        print("[INFO] Khong tao duoc chunk nao.")
        return get_collection_info()

    chunks = embed_chunks(chunks)
    upsert_chunks(chunks)
    upsert_summary_chunk(_car_names_from_chunks(chunks))
    mark_documents_processed(docs)

    info = get_collection_info()
    print(info)
    return info


def _parse_args() -> argparse.Namespace:
    """Parse tham số CLI cho lệnh ingest tài liệu."""
    parser = argparse.ArgumentParser(description="Ingest PDF documents into Qdrant.")
    parser.add_argument(
        "--rebuild",
        action="store_true",
        help="Recreate collection and ingest every PDF from disk.",
    )
    parser.add_argument(
        "--pdf-path",
        action="append",
        dest="pdf_paths",
        help="PDF file or folder to ingest. Can be passed multiple times.",
    )
    parser.add_argument(
        "--glob",
        dest="glob_pattern",
        default=None,
        help="Glob pattern when --pdf-path points to a folder, e.g. '*.pdf'.",
    )
    return parser.parse_args()


def _configure_console_encoding() -> None:
    """Cấu hình stdout/stderr dùng UTF-8 khi chạy CLI trên Windows."""
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(encoding="utf-8", errors="replace")


if __name__ == "__main__":
    _configure_console_encoding()
    args = _parse_args()
    try:
        ingest_documents(
            rebuild=args.rebuild,
            pdf_paths=args.pdf_paths,
            glob_pattern=args.glob_pattern,
        )
    except RuntimeError as exc:
        print(f"[ERROR] {exc}")
        raise SystemExit(1) from exc
