from document_labeler import apply_document_labels, label_document_by_rules


def test_rule_labeler_detects_clear_vehicle_file():
    labels = label_document_by_rules(
        source="TOYOTA SUV.pdf",
        text="Fortuner, Corolla Cross va Yaris Cross co thong so an toan.",
    )

    assert labels.document_category == "suv"
    assert "fortuner" in labels.mentioned_models
    assert "thong_so" in labels.document_tags
    assert "an_toan" in labels.document_tags
    assert labels.label_source == "rule"
    assert labels.label_confidence >= 0.75


def test_apply_document_labels_propagates_one_label_set_to_pages(monkeypatch):
    monkeypatch.delenv("GROQ_API_KEY", raising=False)
    docs = [
        {
            "content": "Khach hang co the dat lich lai thu va mua xe tra gop.",
            "metadata": {"source": "tu-van.pdf", "source_id": "doc-1"},
        },
        {
            "content": "Ho so vay mua xe can giay to tuy than.",
            "metadata": {"source": "tu-van.pdf", "source_id": "doc-1"},
        },
    ]

    labeled = apply_document_labels(docs)

    for doc in labeled:
        metadata = doc["metadata"]
        assert metadata["document_tags"] == ["lai_thu", "tra_gop", "thu_tuc"]
        assert metadata["label_source"] == "rule"
        assert "label_confidence" in metadata
