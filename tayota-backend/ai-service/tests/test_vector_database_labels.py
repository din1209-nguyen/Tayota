from qdrant_client.models import MatchAny

import vector_database
from vector_database import DOCUMENT_CATEGORY_BASIC_ADVICE, _chunk_payload, _source_filter


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


def test_infer_document_category_handles_basic_advice_filename():
    category = vector_database.infer_document_category("file_tư_vấn_cơ_bản_updated.pdf")

    assert category == DOCUMENT_CATEGORY_BASIC_ADVICE


def test_search_neighbor_chunks_can_cross_page_boundary(monkeypatch):
    class FakePoint:
        def __init__(self, payload):
            self.payload = payload

    class FakeClient:
        def scroll(self, **kwargs):
            return (
                [
                    FakePoint(
                        {
                            "chunk_id": "page-9-chunk-0",
                            "content": "Buoc 4: chon dai ly.",
                            "page": 9,
                            "source": "basic.pdf",
                            "source_id": "source",
                            "chunk_index": 0,
                        }
                    ),
                    FakePoint(
                        {
                            "chunk_id": "page-8-chunk-5",
                            "content": "Buoc 1: dang nhap.",
                            "page": 8,
                            "source": "basic.pdf",
                            "source_id": "source",
                            "chunk_index": 5,
                        }
                    ),
                    FakePoint(
                        {
                            "chunk_id": "page-8-chunk-4",
                            "content": "Gioi thieu quy trinh.",
                            "page": 8,
                            "source": "basic.pdf",
                            "source_id": "source",
                            "chunk_index": 4,
                        }
                    ),
                ],
                None,
            )

    monkeypatch.setattr(vector_database, "get_client", lambda: FakeClient())

    neighbors = vector_database.search_neighbor_chunks(
        source_id="source",
        page=8,
        chunk_index=5,
        window=1,
    )

    assert [neighbor["chunk_id"] for neighbor in neighbors] == [
        "page-8-chunk-4",
        "page-9-chunk-0",
    ]


def test_search_neighbor_chunks_page_filter_expands_with_window(monkeypatch):
    captured = {}

    class FakeClient:
        def scroll(self, **kwargs):
            captured.update(kwargs)
            return ([], None)

    monkeypatch.setattr(vector_database, "get_client", lambda: FakeClient())

    vector_database.search_neighbor_chunks(
        source_id="source",
        page=8,
        chunk_index=5,
        window=3,
    )

    page_condition = captured["scroll_filter"].must[1]
    assert page_condition.match.any == [5, 6, 7, 8, 9, 10, 11]


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
