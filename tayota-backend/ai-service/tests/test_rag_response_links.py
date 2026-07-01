from conversation_state_manager import ConversationStateManager
import rag


def test_make_result_appends_response_links_before_return_and_log(monkeypatch):
    state_manager = ConversationStateManager()
    state = state_manager.get_or_create("links-integration")
    monkeypatch.setattr(rag, "state_manager", state_manager)

    def fake_append_links(answer, *, query, intent, sources):
        assert query == "Cho toi thong tin Vios"
        assert intent == "car_info"
        assert sources == [{"source": "sedan.pdf"}]
        return f"{answer}\n\nXem chi tiet xe: http://frontend/vehicles/vios-1"

    monkeypatch.setattr(rag, "append_relevant_links", fake_append_links)

    result = rag._make_result(
        answer="Toyota Vios la mau sedan.",
        sources=[{"source": "sedan.pdf"}],
        model_used="test-model",
        intent="car_info",
        rule_name="",
        state=state,
        session_id="links-integration",
        question="Cho toi thong tin Vios",
    )

    assert "http://frontend/vehicles/vios-1" in result["answer"]
    assert state_manager.chat_messages[-1]["answer"] == result["answer"]
