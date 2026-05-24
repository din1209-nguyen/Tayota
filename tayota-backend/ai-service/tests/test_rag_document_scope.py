from conversation_state_manager import ConversationState
from rag import (
    GENERAL_DOCUMENT_SOURCES,
    HYBRID_DOCUMENT_SOURCES,
    VEHICLE_DOCUMENT_SOURCES,
    _document_scope_for_query,
)


def test_general_catalog_query_uses_only_general_sources():
    state = ConversationState(session_id="scope-1")

    scope, sources = _document_scope_for_query(
        "Toyota hien co nhung dong xe nao?",
        state,
    )

    assert scope == "general"
    assert sources == GENERAL_DOCUMENT_SOURCES
    assert not set(sources) & set(VEHICLE_DOCUMENT_SOURCES)


def test_general_service_query_uses_only_general_sources():
    state = ConversationState(session_id="scope-2")

    scope, sources = _document_scope_for_query(
        "Cho toi biet quy trinh dat lich lai thu va bao duong",
        state,
    )

    assert scope == "general"
    assert sources == GENERAL_DOCUMENT_SOURCES
    assert not set(sources) & set(VEHICLE_DOCUMENT_SOURCES)


def test_specific_vehicle_query_can_use_vehicle_sources():
    state = ConversationState(session_id="scope-3")

    scope, sources = _document_scope_for_query("Gia Toyota Hilux bao nhieu?", state)

    assert scope == "vehicle"
    assert sources == ["Toyota HILUX .pdf"]


def test_vehicle_category_query_is_limited_to_matching_source():
    state = ConversationState(session_id="scope-suv")

    scope, sources = _document_scope_for_query(
        "Toyota co nhung dong xe SUV nao?",
        state,
    )

    assert scope == "vehicle"
    assert sources == ["TOYOTA SUV.pdf"]
    assert "TOYOTA SEDAN.pdf" not in sources


def test_specific_cross_model_does_not_pull_broad_corolla_sedan_source():
    state = ConversationState(session_id="scope-corolla-cross")

    scope, sources = _document_scope_for_query(
        "Cho toi biet thong tin Corolla Cross",
        state,
    )

    assert scope == "vehicle"
    assert sources == ["TOYOTA SUV.pdf"]


def test_need_based_query_still_uses_hybrid_sources():
    state = ConversationState(session_id="scope-4")

    scope, sources = _document_scope_for_query(
        "Toi can tu van xe 7 cho cho gia dinh",
        state,
    )

    assert scope == "hybrid"
    assert sources == HYBRID_DOCUMENT_SOURCES
