import hashlib
import os
import tempfile
import uuid
from datetime import datetime, timezone
from pathlib import Path
from typing import BinaryIO, Dict, Iterable, List, Optional

from dotenv import load_dotenv

load_dotenv()

try:
    import gridfs
    from gridfs.errors import NoFile
    from bson import ObjectId
    from pymongo import MongoClient
    from pymongo.errors import PyMongoError
except ImportError:  # pragma: no cover - only hit before dependencies are installed.
    gridfs = None
    ObjectId = str
    MongoClient = None

    class PyMongoError(Exception):
        pass

    class NoFile(Exception):
        pass


class MongoStorageError(RuntimeError):
    pass


class MongoConnection:
    def __init__(
        self,
        mongo_uri: str | None = None,
        db_name: str | None = None,
    ):
        """Khởi tạo cấu hình kết nối MongoDB dùng chung cho storage."""
        self.mongo_uri = mongo_uri or os.getenv("MONGO_URI", "mongodb://localhost:27017")
        self.db_name = db_name or os.getenv("MONGO_DB", "tayota_ai_db")
        self._client = None
        self._db = None

    def client(self):
        """Khởi tạo lazy MongoClient và tái sử dụng cho các lần gọi sau."""
        if MongoClient is None:
            raise MongoStorageError(
                "MongoDB dependency is not installed. Run `pip install -r requirements.txt`."
            )
        if self._client is None:
            self._client = MongoClient(
                self.mongo_uri,
                serverSelectionTimeoutMS=2000,
                connectTimeoutMS=2000,
            )
        return self._client

    def db(self):
        """Trả về database MongoDB đã cấu hình."""
        if self._db is None:
            self._db = self.client()[self.db_name]
        return self._db

    def ping(self) -> bool:
        """Kiểm tra kết nối MongoDB bằng lệnh ping."""
        try:
            self.client().admin.command("ping")
            return True
        except PyMongoError as exc:
            raise MongoStorageError(
                f"Cannot connect to MongoDB at {self.mongo_uri}: {exc}"
            ) from exc


class MongoDocumentStore:
    def __init__(
        self,
        connection: MongoConnection | None = None,
        *,
        collection_name: str = "ai_documents",
        bucket_name: str | None = None,
    ):
        """Khởi tạo document store dùng Mongo collection và GridFS bucket."""
        self.connection = connection or MongoConnection()
        self.collection_name = collection_name
        self.bucket_name = bucket_name or os.getenv("MONGO_GRIDFS_BUCKET", "ai_pdfs")
        self._bucket = None

    @property
    def documents(self):
        """Trả về collection metadata tài liệu."""
        return self.connection.db()[self.collection_name]

    @property
    def bucket(self):
        """Trả về GridFS bucket lưu nội dung PDF."""
        if gridfs is None:
            raise MongoStorageError(
                "GridFS dependency is not installed. Run `pip install -r requirements.txt`."
            )
        if self._bucket is None:
            self._bucket = gridfs.GridFS(self.connection.db(), collection=self.bucket_name)
        return self._bucket

    def save_pdf(
        self,
        *,
        filename: str,
        content_type: str | None,
        file_obj: BinaryIO,
        uploaded_by_user_id: Optional[str] = None,
    ) -> Dict[str, object]:
        """Lưu PDF upload vào GridFS và metadata vào MongoDB."""
        document_id = str(uuid.uuid4())
        sha256 = hashlib.sha256()
        size_bytes = 0

        with tempfile.SpooledTemporaryFile(max_size=10 * 1024 * 1024) as tmp:
            for chunk in iter(lambda: file_obj.read(1024 * 1024), b""):
                if not chunk:
                    break
                sha256.update(chunk)
                size_bytes += len(chunk)
                tmp.write(chunk)
            tmp.seek(0)

            now = datetime.now(timezone.utc)
            try:
                gridfs_file_id = self.bucket.put(
                    tmp,
                    filename=filename,
                    content_type=content_type or "application/pdf",
                    metadata={
                        "document_id": document_id,
                        "uploaded_by_user_id": uploaded_by_user_id,
                        "sha256": sha256.hexdigest(),
                    },
                )
                metadata = {
                    "document_id": document_id,
                    "gridfs_file_id": gridfs_file_id,
                    "filename": filename,
                    "content_type": content_type or "application/pdf",
                    "size_bytes": size_bytes,
                    "sha256": sha256.hexdigest(),
                    "uploaded_at": now,
                    "uploaded_by_user_id": uploaded_by_user_id,
                    "status": "uploaded",
                }
                self.documents.insert_one(metadata)
            except PyMongoError as exc:
                raise MongoStorageError(f"Cannot save PDF '{filename}' to MongoDB: {exc}") from exc

        return self._public_document(metadata)

    def get_document(self, document_id: str) -> Optional[Dict[str, object]]:
        """Lấy metadata công khai của một tài liệu theo document_id."""
        try:
            document = self.documents.find_one({"document_id": document_id})
        except PyMongoError as exc:
            raise MongoStorageError(f"Cannot load document '{document_id}': {exc}") from exc
        return self._public_document(document) if document else None

    def list_documents(self, *, statuses: Iterable[str] = ("uploaded", "indexed")) -> List[Dict[str, object]]:
        """Liệt kê tài liệu có trạng thái nằm trong danh sách cho phép."""
        try:
            docs = self.documents.find({"status": {"$in": list(statuses)}})
            return [self._public_document(doc) for doc in docs]
        except PyMongoError as exc:
            raise MongoStorageError(f"Cannot list MongoDB documents: {exc}") from exc

    def update_status(self, document_id: str, status: str) -> None:
        """Cập nhật trạng thái ingest/indexing của một tài liệu."""
        try:
            self.documents.update_one(
                {"document_id": document_id},
                {
                    "$set": {
                        "status": status,
                        "updated_at": datetime.now(timezone.utc),
                    }
                },
            )
        except PyMongoError as exc:
            raise MongoStorageError(
                f"Cannot update document '{document_id}' status to '{status}': {exc}"
            ) from exc

    def delete_document(self, document_id: str) -> Optional[Dict[str, object]]:
        """Xóa file PDF trong GridFS và metadata tài liệu khỏi MongoDB."""
        document = self.get_document(document_id)
        if not document:
            return None

        try:
            gridfs_file_id = document.get("gridfs_file_id")
            if gridfs_file_id is not None:
                try:
                    self.bucket.delete(gridfs_file_id)
                except NoFile:
                    pass
            self.documents.delete_one({"document_id": document_id})
        except PyMongoError as exc:
            raise MongoStorageError(f"Cannot delete document '{document_id}': {exc}") from exc

        return document

    def materialize_pdf(self, document_id: str, target_dir: str | Path) -> Path:
        """Ghi PDF từ GridFS ra thư mục tạm để pipeline ingest đọc được."""
        document = self.get_document(document_id)
        if not document:
            raise MongoStorageError(f"Document '{document_id}' was not found.")

        target_parent = Path(target_dir) / str(document["document_id"])
        target_parent.mkdir(parents=True, exist_ok=True)
        target = target_parent / str(document["filename"])
        try:
            grid_out = self.bucket.get(document["gridfs_file_id"])
            with target.open("wb") as out_file:
                for chunk in iter(lambda: grid_out.read(1024 * 1024), b""):
                    if not chunk:
                        break
                    out_file.write(chunk)
        except PyMongoError as exc:
            raise MongoStorageError(
                f"Cannot read PDF '{document_id}' from GridFS: {exc}"
            ) from exc
        return target

    def materialize_all_pdfs(self, target_dir: str | Path) -> List[Path]:
        """Materialize toàn bộ PDF hiện có ra thư mục tạm."""
        return [
            self.materialize_pdf(str(document["document_id"]), target_dir)
            for document in self.list_documents()
        ]

    def _public_document(self, document: Dict[str, object]) -> Dict[str, object]:
        """Loại bỏ trường nội bộ MongoDB khỏi metadata trả ra ngoài."""
        public = dict(document)
        public.pop("_id", None)
        gridfs_file_id = public.get("gridfs_file_id")
        if isinstance(gridfs_file_id, ObjectId):
            public["gridfs_file_id"] = gridfs_file_id
        return public


class MongoDocumentJobStore:
    def __init__(
        self,
        connection: MongoConnection | None = None,
        *,
        collection_name: str = "ai_document_jobs",
    ):
        """Khởi tạo store lưu trạng thái các job ingest tài liệu."""
        self.connection = connection or MongoConnection()
        self.collection_name = collection_name

    @property
    def jobs(self):
        """Trả về collection lưu document ingest jobs."""
        return self.connection.db()[self.collection_name]

    def set(self, status) -> None:
        """Upsert trạng thái mới nhất của một job ingest."""
        payload = (
            status.model_dump()
            if hasattr(status, "model_dump")
            else dict(status)
        )
        now = datetime.now(timezone.utc)
        payload["updated_at"] = now
        created_at = payload.pop("created_at", now)
        try:
            self.jobs.update_one(
                {"job_id": payload["job_id"]},
                {
                    "$set": payload,
                    "$setOnInsert": {"created_at": created_at},
                },
                upsert=True,
            )
        except PyMongoError as exc:
            raise MongoStorageError(f"Cannot save document job to MongoDB: {exc}") from exc

    def get(self, job_id: str) -> Optional[Dict[str, object]]:
        """Lấy trạng thái job ingest theo job_id."""
        try:
            payload = self.jobs.find_one({"job_id": job_id})
        except PyMongoError as exc:
            raise MongoStorageError(f"Cannot load document job from MongoDB: {exc}") from exc
        if not payload:
            return None
        payload.pop("_id", None)
        return payload
