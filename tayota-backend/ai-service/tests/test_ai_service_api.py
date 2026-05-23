from fastapi.testclient import TestClient
from pydantic import ValidationError

from ai_service.main import app
from ai_service.schemas import ChatRequest


client = TestClient(app)


def test_chat_requires_message():
    response = client.post(
        "/api/v1/chat",
        json={"session_id": "s1", "user_id": "u1"},
    )

    assert response.status_code == 422


def test_chat_requires_session_id():
    response = client.post(
        "/api/v1/chat",
        json={"user_id": "u1", "message": "Xin chao"},
    )

    assert response.status_code == 422


def test_chat_request_user_id_is_optional():
    payload = ChatRequest(session_id="s1", message="Xin chao")

    assert payload.user_id is None


def test_chat_request_rejects_blank_user_id_when_provided():
    try:
        ChatRequest(session_id="s1", user_id="", message="Xin chao")
    except ValidationError:
        return

    raise AssertionError("blank user_id should be rejected")
