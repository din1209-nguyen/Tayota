from fastapi.testclient import TestClient

import app as app_module


client = TestClient(
    app_module.app,
    headers={"X-Gateway-Secret": app_module.GATEWAY_INTERNAL_SECRET},
)


def test_gateway_secret_is_required_for_service_apis():
    direct_client = TestClient(app_module.app)

    response = direct_client.get("/health")

    assert response.status_code == 403
    assert response.json() == {"detail": "Gateway secret is required."}


def test_chat_requires_ai_session_header():
    response = client.post(
        "/api/v1/chat",
        json={"message": "Xin chao"},
    )

    assert response.status_code == 422


def test_chat_requires_message():
    response = client.post(
        "/api/v1/chat",
        headers={"X-AI-Session-Id": "s1"},
        json={},
    )

    assert response.status_code == 422


def test_chat_uses_session_header_and_optional_user(monkeypatch):
    captured = {}

    def fake_answer(message, session_id="default", user_id=None):
        captured["message"] = message
        captured["session_id"] = session_id
        captured["user_id"] = user_id
        return {
            "answer": "Chao anh/chị",
            "sources": [],
            "intent": "greeting",
            "stage": "greeting",
            "slots": {},
            "session_id": session_id,
        }

    monkeypatch.setattr(app_module, "answer", fake_answer)

    response = client.post(
        "/api/v1/chat",
        headers={
            "X-AI-Session-Id": "session-1",
            "X-User-Id": "user-1",
        },
        json={"message": "Xin chao"},
    )

    assert response.status_code == 200
    assert response.json()["session_id"] == "session-1"
    assert captured == {
        "message": "Xin chao",
        "session_id": "session-1",
        "user_id": "user-1",
    }


def test_chat_allows_guest_user(monkeypatch):
    captured = {}

    def fake_answer(message, session_id="default", user_id=None):
        captured["user_id"] = user_id
        return {
            "answer": "Chao anh/chị",
            "sources": [],
            "intent": "greeting",
            "stage": "greeting",
            "slots": {},
            "session_id": session_id,
        }

    monkeypatch.setattr(app_module, "answer", fake_answer)

    response = client.post(
        "/api/v1/chat",
        headers={"X-AI-Session-Id": "guest-session"},
        json={"message": "Xin chao"},
    )

    assert response.status_code == 200
    assert captured["user_id"] is None


def test_get_session_messages_returns_chat_logs(monkeypatch):
    captured = {}

    def fake_list_chat_messages(*, session_id, limit=50, offset=0):
        captured["session_id"] = session_id
        captured["limit"] = limit
        captured["offset"] = offset
        return [
            {
                "session_id": session_id,
                "user_id": "user-1",
                "question": "Xe nao 7 cho?",
                "answer": "Toyota co mot so lua chon 7 cho.",
                "intent": "car_advice",
                "stage": "advising",
                "slots_snapshot": {"seats": 7},
                "sources": [{"source": "toyota.pdf", "page": 1}],
                "model_used": "test-model",
                "rule_triggered": "",
                "created_at": "2026-05-24T00:00:00Z",
            }
        ]

    monkeypatch.setattr(app_module.state_manager, "list_chat_messages", fake_list_chat_messages)

    response = client.get("/api/v1/sessions/session-1/messages?limit=20&offset=5")

    assert response.status_code == 200
    assert response.json()["session_id"] == "session-1"
    assert response.json()["count"] == 1
    assert response.json()["messages"][0]["question"] == "Xe nao 7 cho?"
    assert captured == {
        "session_id": "session-1",
        "limit": 20,
        "offset": 5,
    }


def test_get_user_sessions_returns_session_summaries(monkeypatch):
    captured = {}

    def fake_list_user_sessions(*, user_id, limit=50, offset=0):
        captured["user_id"] = user_id
        captured["limit"] = limit
        captured["offset"] = offset
        return [
            {
                "session_id": "session-1",
                "user_id": user_id,
                "stage": "advising",
                "turn_count": 3,
                "last_intent": "car_advice",
                "filled_slots": {"seats": 7},
                "history_len": 6,
                "status": "active",
                "created_at": "2026-05-24T00:00:00Z",
                "updated_at": "2026-05-24T00:10:00Z",
            }
        ]

    monkeypatch.setattr(app_module.state_manager, "list_user_sessions", fake_list_user_sessions)

    response = client.get("/api/v1/users/user-1/sessions?limit=10&offset=2")

    assert response.status_code == 200
    assert response.json()["user_id"] == "user-1"
    assert response.json()["count"] == 1
    assert response.json()["sessions"][0]["session_id"] == "session-1"
    assert response.json()["sessions"][0]["filled_slots"] == {"seats": 7}
    assert captured == {
        "user_id": "user-1",
        "limit": 10,
        "offset": 2,
    }


def test_list_documents_returns_uploaded_documents(monkeypatch):
    captured = {}

    def fake_list_documents(*, statuses):
        captured["statuses"] = statuses
        return [
            {
                "document_id": "doc-1",
                "filename": "toyota.pdf",
                "status": "indexed",
                "document_category": None,
                "content_type": "application/pdf",
                "size_bytes": 123,
                "sha256": "abc",
                "uploaded_by_user_id": "user-1",
                "uploaded_at": "2026-05-24T00:00:00Z",
            }
        ]

    monkeypatch.setattr(app_module.document_store, "list_documents", fake_list_documents)

    response = client.get("/api/v1/documents", headers={"X-User-Role": "ROLE_ADMIN"})

    assert response.status_code == 200
    assert response.json() == {
        "count": 1,
        "documents": [
            {
                    "document_id": "doc-1",
                    "filename": "toyota.pdf",
                    "status": "indexed",
                    "document_category": None,
                    "content_type": "application/pdf",
                    "size_bytes": 123,
                    "sha256": "abc",
                "uploaded_by_user_id": "user-1",
                "uploaded_at": "2026-05-24T00:00:00Z",
                "updated_at": None,
            }
        ],
    }
    assert captured["statuses"] == ("uploaded", "indexing", "indexed", "failed")


def test_list_documents_allows_status_filter(monkeypatch):
    captured = {}

    def fake_list_documents(*, statuses):
        captured["statuses"] = statuses
        return []

    monkeypatch.setattr(app_module.document_store, "list_documents", fake_list_documents)

    response = client.get(
        "/api/v1/documents?status=indexed&status=failed",
        headers={"X-User-Role": "ROLE_ADMIN"},
    )

    assert response.status_code == 200
    assert response.json() == {"count": 0, "documents": []}
    assert captured["statuses"] == ("indexed", "failed")


def test_list_documents_requires_admin_role():
    response = client.get("/api/v1/documents", headers={"X-User-Role": "ROLE_MANAGER"})

    assert response.status_code == 403


def test_upload_document_requires_admin_role():
    response = client.post(
        "/api/v1/documents",
        files={"file": ("toyota.pdf", b"%PDF-test", "application/pdf")},
        headers={"X-User-Role": "ROLE_USER"},
    )

    assert response.status_code == 403


def test_upload_document_allows_admin_role(monkeypatch):
    captured = {}

    def fake_save_pdf(
        *,
        filename,
        content_type,
        file_obj,
        uploaded_by_user_id=None,
        document_category=None,
    ):
        captured["filename"] = filename
        captured["content_type"] = content_type
        captured["content"] = file_obj.read()
        captured["uploaded_by_user_id"] = uploaded_by_user_id
        captured["document_category"] = document_category
        return {
            "document_id": "doc-1",
            "filename": filename,
        }

    def fake_job_set(status):
        captured["job_status"] = status.status
        captured["job_document_id"] = status.document_id

    def fake_run_ingest_job(job_id, document_id, rebuild):
        captured["ingest_document_id"] = document_id
        captured["rebuild"] = rebuild

    monkeypatch.setattr(app_module.document_store, "save_pdf", fake_save_pdf)
    monkeypatch.setattr(app_module.job_store, "set", fake_job_set)
    monkeypatch.setattr(app_module, "_run_ingest_job", fake_run_ingest_job)

    response = client.post(
        "/api/v1/documents",
        files={"file": ("toyota.pdf", b"%PDF-test", "application/pdf")},
        headers={
            "X-User-Id": "admin-1",
            "X-User-Role": "ROLE_ADMIN",
        },
    )

    assert response.status_code == 200
    assert response.json()["status"] == "queued"
    assert captured["filename"] == "toyota.pdf"
    assert captured["content"] == b"%PDF-test"
    assert captured["uploaded_by_user_id"] == "admin-1"
    assert captured["document_category"] is None
    assert captured["job_status"] == "queued"
    assert captured["job_document_id"] == "doc-1"
    assert captured["ingest_document_id"] == "doc-1"
    assert captured["rebuild"] is False


def test_get_document_job_requires_admin_role():
    response = client.get(
        "/api/v1/documents/jobs/job-1",
        headers={"X-User-Role": "ROLE_USER"},
    )

    assert response.status_code == 403


def test_delete_document_removes_qdrant_chunks_then_mongo(monkeypatch):
    calls = []
    document = {
        "document_id": "doc-1",
        "filename": "toyota.pdf",
    }

    def fake_get_document(document_id):
        calls.append(("get", document_id))
        return document

    def fake_delete_chunks(document_id):
        calls.append(("qdrant", document_id))
        return 12

    def fake_delete_document(document_id):
        calls.append(("mongo", document_id))
        return document

    monkeypatch.setattr(app_module.document_store, "get_document", fake_get_document)
    monkeypatch.setattr(app_module, "delete_document_chunks", fake_delete_chunks)
    monkeypatch.setattr(app_module.document_store, "delete_document", fake_delete_document)

    response = client.delete(
        "/api/v1/documents/doc-1",
        headers={"X-User-Role": "ROLE_ADMIN"},
    )

    assert response.status_code == 200
    assert response.json() == {
        "document_id": "doc-1",
        "filename": "toyota.pdf",
        "status": "deleted",
        "deleted_chunks": 12,
    }
    assert calls == [
        ("get", "doc-1"),
        ("qdrant", "doc-1"),
        ("mongo", "doc-1"),
    ]


def test_delete_document_requires_admin_role():
    response = client.delete(
        "/api/v1/documents/doc-1",
        headers={"X-User-Role": "ROLE_USER"},
    )

    assert response.status_code == 403


def test_delete_document_returns_404_when_missing(monkeypatch):
    monkeypatch.setattr(app_module.document_store, "get_document", lambda document_id: None)

    response = client.delete(
        "/api/v1/documents/missing",
        headers={"X-User-Role": "ROLE_ADMIN"},
    )

    assert response.status_code == 404
