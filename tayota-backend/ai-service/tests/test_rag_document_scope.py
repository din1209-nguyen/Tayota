from conversation_state_manager import ConversationState
from rag import (
    GENERAL_DOCUMENT_CATEGORIES,
    HYBRID_DOCUMENT_CATEGORIES,
    VEHICLE_DOCUMENT_CATEGORIES,
    _document_scope_for_query,
    _history_messages_for_generation,
    _is_offroad_need,
)


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
