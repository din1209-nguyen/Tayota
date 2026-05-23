from fastapi.testclient import TestClient

from app import app


client = TestClient(app)


def test_chat_requires_session_id():
    response = client.post(
        "/api/v1/chat",
        json={"user_id": "u1", "message": "Xin chao"},
    )

    assert response.status_code == 422


def test_chat_requires_message():
    response = client.post(
        "/api/v1/chat",
        json={"session_id": "s1", "user_id": "u1"},
    )

    assert response.status_code == 422
