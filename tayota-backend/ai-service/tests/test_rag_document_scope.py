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
    assert "Toyota HILUX .pdf" in sources


def test_need_based_query_still_uses_hybrid_sources():
    state = ConversationState(session_id="scope-4")

    scope, sources = _document_scope_for_query(
        "Toi can tu van xe 7 cho cho gia dinh",
        state,
    )

    assert scope == "hybrid"
    assert sources == HYBRID_DOCUMENT_SOURCES
