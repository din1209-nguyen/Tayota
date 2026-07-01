from conversation_state_manager import ConversationStateManager
import rag


def test_extract_compare_vehicles_from_vietnamese_query():
    vehicles = rag._extract_compare_vehicles(
        "So sánh Mazda CX-5 và Hyundai Tucson"
    )

    assert vehicles == ["Mazda CX-5", "Hyundai Tucson"]


def test_answer_uses_separate_retrieval_for_compare_vehicle(monkeypatch):
    retrieved_for = []

    def fake_retrieve(vehicle_name, *, query):
        retrieved_for.append(vehicle_name)
        return [
            {
                "source": f"{vehicle_name}.pdf",
                "document_category": "vehicle",
                "page": 1,
                "score": 0.9,
                "content": f"Thong tin ve {vehicle_name}",
                "chunk_id": f"{vehicle_name}-1",
                "chunk_index": 0,
                "mentioned_models": [vehicle_name.lower()],
            }
        ]

    def fake_generate(messages):
        prompt = messages[-1]["content"]
        assert "Xe so sánh: Mazda CX-5" in prompt
        assert "Xe so sánh: Hyundai Tucson" in prompt
        return "Mazda CX-5 va Hyundai Tucson co khac biet theo du lieu.", "test-model"

    monkeypatch.setattr(rag, "state_manager", ConversationStateManager())
    monkeypatch.setattr(
        rag,
        "classify_intent",
        lambda query: {"intent": "out_of_scope", "confidence": 0.8, "reason": "test"},
    )
    monkeypatch.setattr(rag.rules_engine, "check", lambda query, intent: (False, "", ""))
    monkeypatch.setattr(rag, "_retrieve_compare_vehicle_docs", fake_retrieve)
    monkeypatch.setattr(rag, "_generate", fake_generate)

    result = rag.answer(
        "So sánh Mazda CX-5 và Hyundai Tucson",
        session_id="compare-vehicles",
    )

    assert result["intent"] == "compare_vehicle"
    assert retrieved_for == ["Mazda CX-5", "Hyundai Tucson"]
    assert {source["compared_vehicle"] for source in result["sources"]} == {
        "Mazda CX-5",
        "Hyundai Tucson",
    }
