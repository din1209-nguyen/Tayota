from conversation_state_manager import ConversationState
import rag


def test_fuel_saving_query_uses_hybrid_sources():
    state = ConversationState(session_id="fuel-saving")

    scope, categories = rag._document_scope_for_query("xe tiết kiệm xăng", state)

    assert scope == "hybrid"
    assert categories == rag.HYBRID_DOCUMENT_CATEGORIES


def test_query_tags_detect_price_and_fuel_terms():
    state = ConversationState(session_id="tags")

    assert rag._preferred_document_tags_for_query("bảng giá khởi điểm xe", state) == [
        "gia_xe"
    ]
    assert rag._preferred_document_tags_for_query("xe tiết kiệm xăng", state) == [
        "thong_so"
    ]


def test_lexical_support_can_match_business_terms_without_model(monkeypatch):
    state = ConversationState(session_id="lexical-no-model")
    docs = [
        {
            "score": 0.0,
            "content": "Toyota Vios co muc tieu thu nhien lieu tiet kiem.",
            "page": 1,
            "source": "sedan.pdf",
            "document_category": "sedan",
            "document_tags": ["thong_so"],
            "mentioned_models": ["vios"],
            "chunk_id": "vios-fuel",
            "chunk_index": 0,
        },
        {
            "score": 0.0,
            "content": "Thong tin dai ly Toyota.",
            "page": 2,
            "source": "basic.pdf",
            "document_category": "basic_advice",
            "chunk_id": "dealer",
            "chunk_index": 0,
        },
    ]
    monkeypatch.setattr(rag, "scroll_chunks", lambda **kwargs: docs)

    result = rag._lexical_support_docs(
        "xe tiết kiệm xăng",
        state,
        rag.HYBRID_DOCUMENT_CATEGORIES,
        [],
    )

    assert [doc["chunk_id"] for doc in result] == ["vios-fuel"]


def test_metadata_support_uses_document_tags(monkeypatch):
    state = ConversationState(session_id="metadata-tags")
    docs = [
        {
            "score": 0.0,
            "content": "Bang gia khoi diem Toyota Vios.",
            "page": 1,
            "source": "sedan.pdf",
            "document_category": "sedan",
            "document_tags": ["gia_xe"],
            "mentioned_models": ["vios"],
            "chunk_id": "vios-price",
            "chunk_index": 0,
        }
    ]
    monkeypatch.setattr(rag, "scroll_chunks", lambda **kwargs: docs)

    result = rag._metadata_support_docs(
        "bảng giá khởi điểm xe",
        state,
        rag.HYBRID_DOCUMENT_CATEGORIES,
        [],
    )

    assert [doc["chunk_id"] for doc in result] == ["vios-price"]
    assert result[0]["metadata_match"] is True
