from conversation_state_manager import ConversationState
from rag import (
    GENERAL_DOCUMENT_CATEGORIES,
    HYBRID_DOCUMENT_CATEGORIES,
    VEHICLE_DOCUMENT_CATEGORIES,
    _document_scope_for_query,
    _history_messages_for_generation,
    _is_offroad_need,
    _preferred_document_tags_for_query,
    _lexical_support_docs,
    _metadata_support_docs,
    _expand_with_neighbor_context,
    _rerank_retrieved_docs,
)
import rag


def test_general_catalog_query_uses_only_general_sources():
    state = ConversationState(session_id="scope-1")

    scope, categories = _document_scope_for_query(
        "Toyota hien co nhung dong xe nao?",
        state,
    )

    assert scope == "general"
    assert categories == GENERAL_DOCUMENT_CATEGORIES
    assert not set(categories) & set(VEHICLE_DOCUMENT_CATEGORIES)


def test_general_service_query_uses_only_general_sources():
    state = ConversationState(session_id="scope-2")

    scope, categories = _document_scope_for_query(
        "Cho toi biet quy trinh dat lich lai thu va bao duong",
        state,
    )

    assert scope == "general"
    assert categories == GENERAL_DOCUMENT_CATEGORIES
    assert not set(categories) & set(VEHICLE_DOCUMENT_CATEGORIES)


def test_specific_vehicle_query_can_use_vehicle_sources():
    state = ConversationState(session_id="scope-3")

    scope, categories = _document_scope_for_query("Gia Toyota Hilux bao nhieu?", state)

    assert scope == "vehicle"
    assert categories == ["hilux"]


def test_vehicle_category_query_is_limited_to_matching_source():
    state = ConversationState(session_id="scope-suv")

    scope, categories = _document_scope_for_query(
        "Toyota co nhung dong xe SUV nao?",
        state,
    )

    assert scope == "vehicle"
    assert categories == ["suv"]
    assert "sedan" not in categories


def test_specific_cross_model_does_not_pull_broad_corolla_sedan_source():
    state = ConversationState(session_id="scope-corolla-cross")

    scope, categories = _document_scope_for_query(
        "Cho toi biet thong tin Corolla Cross",
        state,
    )

    assert scope == "vehicle"
    assert categories == ["suv"]


def test_need_based_query_still_uses_hybrid_sources():
    state = ConversationState(session_id="scope-4")

    scope, categories = _document_scope_for_query(
        "Toi can tu van xe 7 cho cho gia dinh",
        state,
    )

    assert scope == "hybrid"
    assert categories == HYBRID_DOCUMENT_CATEGORIES


def test_offroad_need_accepts_unsigned_vietnamese_query():
    state = ConversationState(session_id="scope-offroad")

    assert _is_offroad_need("Toi hay di dia hinh, duong xau va leo nui", state)


def test_offroad_need_accepts_unsigned_slot_value():
    state = ConversationState(session_id="scope-offroad-slot")
    state.slots["region"] = "dia hinh"

    assert _is_offroad_need("Toi can tu van xe", state)


def test_generation_history_is_skipped_for_standalone_vehicle_query():
    state = ConversationState(session_id="history-independent")
    state.add_turn("Toi can xe 7 cho cho gia dinh", "Toyota Innova Cross phu hop.")

    history = _history_messages_for_generation("Gia Vios bao nhieu?", state)

    assert history == []


def test_generation_history_is_kept_for_contextual_query():
    state = ConversationState(session_id="history-contextual")
    state.add_turn("Cho toi biet ve Vios", "Vios la mau sedan.")

    history = _history_messages_for_generation("Xe nay co may phien ban?", state)

    assert [message["role"] for message in history] == ["user", "assistant"]


def test_query_tags_detect_price_and_specs():
    state = ConversationState(session_id="tags")

    tags = _preferred_document_tags_for_query("Fortuner co gia va thong so gi?", state)

    assert tags == ["gia_xe", "thong_so"]


def test_rerank_prefers_matching_document_tag_and_model_metadata():
    state = ConversationState(session_id="rerank-tags")
    docs = [
        {
            "score": 0.5,
            "content": "Noi dung chung ve Toyota.",
            "page": 1,
            "source": "basic.pdf",
            "document_category": "basic_advice",
            "chunk_id": "general",
            "chunk_index": 0,
        },
        {
            "score": 0.5,
            "content": "Bang gia va thong so.",
            "page": 1,
            "source": "suv.pdf",
            "document_category": "suv",
            "document_tags": ["gia_xe", "thong_so"],
            "mentioned_models": ["fortuner"],
            "chunk_id": "fortuner",
            "chunk_index": 0,
        },
    ]

    ranked = _rerank_retrieved_docs(
        docs,
        query="Fortuner co gia va thong so gi?",
        state=state,
        document_scope="vehicle",
        offroad_need=False,
        limit=2,
    )

    assert ranked[0]["chunk_id"] == "fortuner"
    assert ranked[0]["rerank_score"] > ranked[1]["rerank_score"]


def test_support_docs_use_configured_limits(monkeypatch):
    state = ConversationState(session_id="support-limit")
    calls = []

    def fake_scroll_chunks(**kwargs):
        calls.append(kwargs)
        return []

    monkeypatch.setattr(rag, "scroll_chunks", fake_scroll_chunks)

    _lexical_support_docs(
        "Gia Fortuner bao nhieu?",
        state,
        ["suv"],
        ["TOYOTA SUV.pdf"],
        limit=7,
    )
    _metadata_support_docs(
        "Fortuner co thong so gi?",
        state,
        ["suv"],
        ["TOYOTA SUV.pdf"],
        limit=9,
    )

    assert calls[0]["limit"] == 7
    assert calls[1]["limit"] == 9


def test_neighbor_expansion_uses_only_top_seed_docs(monkeypatch):
    calls = []

    def fake_search_neighbor_chunks(**kwargs):
        calls.append(kwargs)
        return []

    monkeypatch.setattr(rag, "search_neighbor_chunks", fake_search_neighbor_chunks)
    docs = [
        {
            "chunk_id": f"chunk-{idx}",
            "source_id": "source",
            "page": 1,
            "chunk_index": idx,
            "content": f"content {idx}",
            "score": 0.9,
        }
        for idx in range(5)
    ]

    expanded = _expand_with_neighbor_context(docs, seed_limit=2)

    assert len(calls) == 2
    assert [doc["chunk_id"] for doc in expanded] == ["chunk-0", "chunk-1"]

