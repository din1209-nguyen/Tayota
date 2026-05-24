from io import BytesIO

from mongo_storage import MongoDocumentJobStore, MongoDocumentStore


class FakeInsertResult:
    pass


class FakeUpdateResult:
    pass


class FakeDeleteResult:
    pass


class FakeCollection:
    def __init__(self):
        self.data = {}
        self.inserted = []

    def insert_one(self, document):
        saved = dict(document)
        self.inserted.append(saved)
        key = saved.get("document_id") or saved.get("job_id") or len(self.inserted)
        self.data[key] = saved
        return FakeInsertResult()

    def find_one(self, query):
        key = query.get("document_id") or query.get("job_id")
        value = self.data.get(key)
        return dict(value) if value else None

    def find(self, query):
        statuses = set(query.get("status", {}).get("$in", []))
        return [
            dict(item)
            for item in self.data.values()
            if not statuses or item.get("status") in statuses
        ]

    def update_one(self, query, update, upsert=False):
        key = query.get("document_id") or query.get("job_id")
        current = self.data.get(key, {})
        if upsert and not current:
            current.update(update.get("$setOnInsert", {}))
        current.update(update.get("$set", {}))
        self.data[key] = current
        return FakeUpdateResult()

    def delete_one(self, query):
        key = query.get("document_id") or query.get("job_id")
        self.data.pop(key, None)
        return FakeDeleteResult()


class FakeBucket:
    def __init__(self):
        self.files = {}

    def put(self, file_obj, **kwargs):
        file_id = "grid-1"
        self.files[file_id] = {
            "content": file_obj.read(),
            "kwargs": kwargs,
        }
        return file_id

    def delete(self, file_id):
        self.files.pop(file_id, None)


class FakeDocumentStore(MongoDocumentStore):
    def __init__(self):
        self.fake_documents = FakeCollection()
        self.fake_bucket = FakeBucket()
        super().__init__(connection=None)

    @property
    def documents(self):
        return self.fake_documents

    @property
    def bucket(self):
        return self.fake_bucket


class FakeJobStore(MongoDocumentJobStore):
    def __init__(self):
        self.fake_jobs = FakeCollection()
        super().__init__(connection=None)

    @property
    def jobs(self):
        return self.fake_jobs


def test_document_store_saves_pdf_metadata_and_gridfs_file():
    store = FakeDocumentStore()

    document = store.save_pdf(
        filename="toyota.pdf",
        content_type="application/pdf",
        file_obj=BytesIO(b"%PDF-test"),
        uploaded_by_user_id="u1",
    )

    assert document["filename"] == "toyota.pdf"
    assert document["gridfs_file_id"] == "grid-1"
    assert document["size_bytes"] == len(b"%PDF-test")
    assert document["uploaded_by_user_id"] == "u1"
    assert store.fake_bucket.files["grid-1"]["content"] == b"%PDF-test"


def test_document_store_deletes_metadata_and_gridfs_file():
    store = FakeDocumentStore()
    document = store.save_pdf(
        filename="toyota.pdf",
        content_type="application/pdf",
        file_obj=BytesIO(b"%PDF-test"),
        uploaded_by_user_id="u1",
    )

    deleted = store.delete_document(document["document_id"])

    assert deleted["document_id"] == document["document_id"]
    assert store.get_document(document["document_id"]) is None
    assert store.fake_bucket.files == {}


def test_document_store_delete_missing_document_returns_none():
    store = FakeDocumentStore()

    assert store.delete_document("missing") is None


def test_document_job_store_round_trips_status_dict():
    store = FakeJobStore()

    store.set(
        {
            "job_id": "job-1",
            "status": "queued",
            "message": "Queued",
            "document_id": "doc-1",
            "indexed_pages": 0,
            "indexed_chunks": 0,
        }
    )

    loaded = store.get("job-1")

    assert loaded["job_id"] == "job-1"
    assert loaded["status"] == "queued"
    assert loaded["document_id"] == "doc-1"
