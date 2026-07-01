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


def test_rule_labeler_keeps_vehicle_docs_out_of_basic_advice():
    labels = label_document_by_rules(
        source="TOYOTA SUV.pdf",
        text="Toyota Fortuner phu hop tu van mua xe gia dinh va di duong dai.",
    )

    assert labels.document_category == "suv"


def test_rule_labeler_detects_fuel_consumption_specs():
    labels = label_document_by_rules(
        source="TOYOTA SEDAN.pdf",
        text="Muc tieu thu nhien lieu tiet kiem xang 5.2 lit/100km.",
    )

    assert "thong_so" in labels.document_tags


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
        assert "lai_thu" in metadata["document_tags"]
        assert "tra_gop" in metadata["document_tags"]
        assert "thu_tuc" in metadata["document_tags"]
        assert metadata["label_source"] == "rule"
        assert "label_confidence" in metadata


def test_rule_labeler_detects_service_and_breakdown_tags():
    labels = label_document_by_rules(
        source="file tu van co ban.pdf",
        text=(
            "[LAI THU] Khach hang co the dat lich lai thu Toyota. "
            "[BAO DUONG] Lich bao duong dinh ky tai dai ly. "
            "[SU CO] Neu xe bi hong giua duong, goi hotline de duoc cuu ho "
            "va dua xe den xuong dich vu sua chua."
        ),
    )

    assert labels.document_category == "basic_advice"
    assert "lai_thu" in labels.document_tags
    assert "bao_duong" in labels.document_tags
    assert "bao_hanh" in labels.document_tags


def test_rule_labeler_detects_procedure_guidance_as_thu_tuc():
    labels = label_document_by_rules(
        source="file tu van co ban.pdf",
        text=(
            "[BAO DUONG DAT LICH] Huong dan quy trinh dat lich bao duong "
            "gom cac buoc dang nhap, chon dich vu va xac nhan lich hen."
        ),
    )

    assert labels.document_category == "basic_advice"
    assert "bao_duong" in labels.document_tags
    assert "thu_tuc" in labels.document_tags


def test_apply_document_labels_samples_beginning_middle_and_end(monkeypatch):
    monkeypatch.delenv("GROQ_API_KEY", raising=False)
    docs = [
        {
            "content": f"Trang {index} noi dung chung.",
            "metadata": {"source": "file tu van co ban.pdf", "source_id": "doc-long"},
        }
        for index in range(14)
    ]
    docs[-1]["content"] = (
        "[SU CO] Xe bi hong giua duong thi goi hotline ho tro cuu ho "
        "va den xuong dich vu Toyota de sua chua."
    )

    labeled = apply_document_labels(docs)

    for doc in labeled:
        metadata = doc["metadata"]
        assert metadata["document_category"] == "basic_advice"
        assert "bao_duong" in metadata["document_tags"]
        assert "bao_hanh" in metadata["document_tags"]
