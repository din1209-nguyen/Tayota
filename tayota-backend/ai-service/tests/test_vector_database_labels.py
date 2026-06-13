from qdrant_client.models import MatchAny

import vector_database
from vector_database import _chunk_payload, _source_filter


def test_chunk_payload_includes_auto_label_metadata():
    payload = _chunk_payload(
        {
            "chunk_id": "chunk-1",
            "content": "Gia xe Fortuner va thong so an toan.",
            "metadata": {
                "source": "TOYOTA SUV.pdf",
                "document_category": "suv",
                "document_tags": ["gia_xe", "thong_so"],
                "mentioned_models": ["fortuner"],
                "label_source": "rule",
                "label_confidence": 0.9,
                "page": 1,
            },
        }
    )

    assert payload["document_category"] == "suv"
    assert payload["document_tags"] == ["gia_xe", "thong_so"]
    assert payload["mentioned_models"] == ["fortuner"]
    assert payload["label_source"] == "rule"
    assert payload["label_confidence"] == 0.9


def test_source_filter_can_match_tags_and_models():
    query_filter = _source_filter(
        ["TOYOTA SUV.pdf"],
        ["suv"],
        document_tags=["gia_xe"],
        mentioned_models=["fortuner"],
    )

    conditions = query_filter.should
    matches = {
        condition.key: condition.match
        for condition in conditions
        if isinstance(condition.match, MatchAny)
    }

    assert matches["document_category"].any == ["suv"]
    assert matches["source"].any == ["TOYOTA SUV.pdf"]
    assert matches["document_tags"].any == ["gia_xe"]
    assert matches["mentioned_models"].any == ["fortuner"]


def test_get_client_reuses_singleton(monkeypatch):
    created = []

    class FakeClient:
        def __init__(self, **kwargs):
            created.append(kwargs)

    monkeypatch.setattr(vector_database, "_client", None)
    monkeypatch.setattr(vector_database, "QdrantClient", FakeClient)

    first = vector_database.get_client()
    second = vector_database.get_client()

    assert first is second
    assert len(created) == 1
