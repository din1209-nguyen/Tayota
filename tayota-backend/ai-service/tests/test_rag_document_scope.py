from conversation_state_manager import ConversationState, ConversationStateManager
from rag import (
    GENERAL_DOCUMENT_CATEGORIES,
    HYBRID_DOCUMENT_CATEGORIES,
    DOCUMENT_CATEGORY_BASIC_ADVICE,
    VEHICLE_DOCUMENT_CATEGORIES,
    _document_scope_for_query,
    _build_retrieval_query,
    _history_messages_for_generation,
    _is_offroad_need,
    _preferred_document_tags_for_query,
    _lexical_support_docs,
    _metadata_support_docs,
    _expand_with_neighbor_context,
    _rerank_retrieved_docs,
    _try_build_procedure_answer,
    _try_build_price_answer,
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


def test_vehicle_breakdown_query_uses_only_general_sources():
    state = ConversationState(session_id="scope-breakdown")

    scope, categories = _document_scope_for_query(
        "xe bi hong thi nen lam gi",
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


def test_fuel_saving_preference_uses_hybrid_sources():
    state = ConversationState(session_id="scope-fuel-saving")

    scope, categories = _document_scope_for_query("uu tien tiet kiem xang", state)

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


def test_procedure_query_does_not_use_previous_wrong_answer_as_history():
    state = ConversationState(session_id="history-procedure")
    state.add_turn(
        "huong dan dat lich bao duong",
        "Lien he dai ly Toyota gan nhat de dat lich hen bao duong.",
    )

    history = _history_messages_for_generation("huong dan dat lich bao duong", state)
    retrieval_query = _build_retrieval_query(
        "huong dan dat lich bao duong",
        state,
        include_slots=False,
    )

    assert history == []
    assert "Lien he dai ly" not in retrieval_query


def test_query_tags_detect_price_and_specs():
    state = ConversationState(session_id="tags")

    tags = _preferred_document_tags_for_query("Fortuner co gia va thong so gi?", state)

    assert tags == ["gia_xe", "thong_so"]


def test_query_tags_detect_service_questions():
    state = ConversationState(session_id="service-tags")

    assert _preferred_document_tags_for_query("huong dan dat lich lai thu", state) == [
        "lai_thu",
        "thu_tuc",
    ]
    assert _preferred_document_tags_for_query("huong dan dat lich bao duong", state) == [
        "bao_duong",
        "thu_tuc",
    ]
    assert _preferred_document_tags_for_query("toi can sua chua xe", state) == [
        "bao_duong",
        "bao_hanh",
    ]
    assert _preferred_document_tags_for_query("xe bi hong thi nen lam gi", state) == [
        "bao_duong",
        "bao_hanh",
    ]


def test_rerank_prefers_exact_service_procedure_chunk():
    state = ConversationState(session_id="rerank-service-procedure")
    docs = [
        {
            "score": 0.65,
            "content": "Lien he truc tiep dai ly de duoc tu van bao duong xe Toyota.",
            "page": 1,
            "source": "basic.pdf",
            "document_category": DOCUMENT_CATEGORY_BASIC_ADVICE,
            "document_tags": ["bao_duong"],
            "chunk_id": "generic-maintenance",
            "chunk_index": 0,
        },
        {
            "score": 0.45,
            "content": (
                "[BAO DUONG DAT LICH] Quy trinh dat lich bao duong xe Toyota truc tuyen. "
                "Buoc 1: dang nhap tai khoan Toyota. "
                "Buoc 2: chon xe can bao duong. "
                "Buoc 3: chon loai dich vu bao duong. "
                "Buoc 4: chon dai ly Toyota. "
                "Buoc 5: chon ngay va khung gio mong muon. "
                "Buoc 6: xac nhan thong tin va hoan tat dat lich."
            ),
            "page": 2,
            "source": "basic.pdf",
            "document_category": DOCUMENT_CATEGORY_BASIC_ADVICE,
            "document_tags": ["bao_duong", "thu_tuc"],
            "chunk_id": "booking-procedure",
            "chunk_index": 1,
        },
    ]

    ranked = _rerank_retrieved_docs(
        docs,
        query="quy trinh dat lich bao duong",
        state=state,
        document_scope="general",
        offroad_need=False,
        limit=2,
    )

    assert ranked[0]["chunk_id"] == "booking-procedure"


def test_rerank_prefers_three_step_procedure_without_fixed_step_count():
    state = ConversationState(session_id="rerank-three-step-procedure")
    docs = [
        {
            "score": 0.6,
            "content": "Thong tin chung ve dich vu bao duong Toyota.",
            "page": 1,
            "source": "basic.pdf",
            "document_category": DOCUMENT_CATEGORY_BASIC_ADVICE,
            "document_tags": ["bao_duong"],
            "chunk_id": "generic-service",
            "chunk_index": 0,
        },
        {
            "score": 0.45,
            "content": (
                "Quy trinh dat lich bao duong nhanh gom 3 buoc. "
                "Buoc 1: dang nhap tai khoan Toyota. "
                "Buoc 2: chon xe va dich vu. "
                "Buoc 3: xac nhan lich hen."
            ),
            "page": 2,
            "source": "basic.pdf",
            "document_category": DOCUMENT_CATEGORY_BASIC_ADVICE,
            "document_tags": ["bao_duong", "thu_tuc"],
            "chunk_id": "three-step-procedure",
            "chunk_index": 1,
        },
    ]

    ranked = _rerank_retrieved_docs(
        docs,
        query="huong dan dat lich bao duong",
        state=state,
        document_scope="general",
        offroad_need=False,
        limit=2,
    )

    assert ranked[0]["chunk_id"] == "three-step-procedure"


def test_rerank_accepts_seven_step_procedure_without_six_step_cap():
    state = ConversationState(session_id="rerank-seven-step-procedure")
    docs = [
        {
            "score": 0.6,
            "content": "Lien he dai ly de duoc tu van bao duong Toyota.",
            "page": 1,
            "source": "basic.pdf",
            "document_category": DOCUMENT_CATEGORY_BASIC_ADVICE,
            "document_tags": ["bao_duong"],
            "chunk_id": "generic-maintenance",
            "chunk_index": 0,
        },
        {
            "score": 0.42,
            "content": (
                "Quy trinh dat lich bao duong Toyota gom 7 buoc. "
                "Buoc 1: dang nhap. Buoc 2: chon xe. Buoc 3: chon dich vu. "
                "Buoc 4: chon dai ly. Buoc 5: chon thoi gian. "
                "Buoc 6: kiem tra thong tin. Buoc 7: xac nhan lich hen."
            ),
            "page": 2,
            "source": "basic.pdf",
            "document_category": DOCUMENT_CATEGORY_BASIC_ADVICE,
            "document_tags": ["bao_duong", "thu_tuc"],
            "chunk_id": "seven-step-procedure",
            "chunk_index": 1,
        },
    ]

    ranked = _rerank_retrieved_docs(
        docs,
        query="quy trinh dat lich bao duong",
        state=state,
        document_scope="general",
        offroad_need=False,
        limit=2,
    )

    assert ranked[0]["chunk_id"] == "seven-step-procedure"


def test_procedure_answer_from_basic_advice_pdf_has_all_maintenance_steps(monkeypatch):
    import fitz

    monkeypatch.setattr(rag, "scroll_chunks", lambda **kwargs: [])
    pdf = fitz.open("documents/file_tư_vấn_cơ_bản_updated.pdf")
    try:
        docs = [
            {
                "content": pdf[page - 1].get_text("text"),
                "page": page,
                "chunk_index": 0,
                "score": 0.9,
            }
            for page in (8, 9)
        ]
    finally:
        pdf.close()

    answer_text = _try_build_procedure_answer("hướng dẫn đặt lịch bảo dưỡng", docs)

    assert answer_text is not None
    assert "Quy trình đặt lịch bảo dưỡng Toyota trực tuyến gồm 6 bước:" in answer_text
    assert "1. đăng nhập tài khoản Toyota" in answer_text
    assert "2. chọn xe cần bảo dưỡng" in answer_text
    assert "3. chọn loại dịch vụ bảo dưỡng như bảo dưỡng định kỳ, thay dầu, kiểm tra tổng quát hoặc sửa chữa theo yêu cầu." in answer_text
    assert "4. chọn đại lý Toyota thực hiện bảo dưỡng." in answer_text
    assert "5. chọn ngày và khung giờ mong muốn" in answer_text
    assert "6. xác nhận thông tin và hoàn tất đặt lịch." in answer_text
    assert "Sau khi đặt lịch thành công" not in answer_text


def test_procedure_answer_parses_steps_split_across_chunks(monkeypatch):
    monkeypatch.setattr(rag, "scroll_chunks", lambda **kwargs: [])
    docs = [
        {
            "content": (
                "[BAO DUONG] Quy trinh dat lich bao duong Toyota truc tuyen gom 6 buoc. "
                "Buoc 1: dang nhap tai khoan Toyota. Buoc 2: chon xe can bao duong. "
                "Buoc 3: chon loai dich vu bao duong nhu"
            ),
            "page": 8,
            "chunk_index": 5,
            "score": 0.9,
        },
        {
            "content": (
                "bao duong dinh ky, thay dau hoac sua chua theo yeu cau. "
                "Buoc 4: chon dai ly Toyota. Buoc 5: chon ngay va khung gio. "
                "Buoc 6: xac nhan thong tin va hoan tat dat lich."
            ),
            "page": 9,
            "chunk_index": 0,
            "score": 0.9,
        },
    ]

    answer_text = _try_build_procedure_answer("huong dan dat lich bao duong", docs)

    assert answer_text is not None
    assert "gom 6 buoc:" in answer_text
    assert "3. chon loai dich vu bao duong nhu bao duong dinh ky" in answer_text
    assert "6. xac nhan thong tin va hoan tat dat lich." in answer_text


def test_procedure_answer_does_not_hardcode_step_count(monkeypatch):
    monkeypatch.setattr(rag, "scroll_chunks", lambda **kwargs: [])
    three_step = [
        {
            "content": (
                "Quy trinh dat lich bao duong nhanh gom 3 buoc. "
                "Buoc 1: dang nhap. Buoc 2: chon dich vu. Buoc 3: xac nhan."
            ),
            "page": 1,
            "chunk_index": 0,
            "score": 0.9,
        }
    ]
    seven_step = [
        {
            "content": (
                "Quy trinh dat lich bao duong chi tiet gom 7 buoc. "
                "Buoc 1: dang nhap. Buoc 2: chon xe. Buoc 3: chon dich vu. "
                "Buoc 4: chon dai ly. Buoc 5: chon thoi gian. "
                "Buoc 6: kiem tra thong tin. Buoc 7: xac nhan."
            ),
            "page": 1,
            "chunk_index": 0,
            "score": 0.9,
        }
    ]

    assert _try_build_procedure_answer("huong dan dat lich bao duong", three_step).count("\n") == 3
    seven_answer = _try_build_procedure_answer("huong dan dat lich bao duong", seven_step)
    assert seven_answer.count("\n") == 7
    assert "7. xac nhan." in seven_answer


def test_procedure_answer_does_not_pick_test_drive_for_maintenance(monkeypatch):
    monkeypatch.setattr(rag, "scroll_chunks", lambda **kwargs: [])
    docs = [
        {
            "content": (
                "Quy trinh dat lich lai thu Toyota gom 5 buoc. "
                "Buoc 1: cung cap thong tin. Buoc 2: chon xe. Buoc 3: chon dai ly. "
                "Buoc 4: chon ngay gio. Buoc 5: xac nhan.\n"
                "[BAO DUONG] Quy trinh dat lich bao duong Toyota gom 3 buoc. "
                "Buoc 1: dang nhap. Buoc 2: chon dich vu bao duong. Buoc 3: xac nhan."
            ),
            "page": 1,
            "chunk_index": 0,
            "score": 0.9,
        }
    ]

    answer_text = _try_build_procedure_answer("huong dan dat lich bao duong", docs)

    assert "bao duong" in answer_text.lower()
    assert "lai thu" not in answer_text.lower()


def test_procedure_answer_requires_topic_match_in_title(monkeypatch):
    monkeypatch.setattr(rag, "scroll_chunks", lambda **kwargs: [])
    docs = [
        {
            "content": (
                "Quy trinh mua xe Toyota gom 2 buoc. "
                "Buoc 1: chon xe. Buoc 2: ky hop dong. "
                "Sau khi mua xe, khach hang nen bao duong dinh ky."
            ),
            "page": 1,
            "chunk_index": 0,
            "score": 0.9,
        }
    ]

    assert _try_build_procedure_answer("huong dan dat lich bao duong", docs) is None


def _patch_answer_basics(monkeypatch, intent="car_advice", confidence=0.9):
    monkeypatch.setattr(rag, "state_manager", ConversationStateManager())
    monkeypatch.setattr(rag, "CHAT_LOG_SYNC", False)
    monkeypatch.setattr(rag, "RAG_AMBIGUITY_LLM_ENABLED", False)
    monkeypatch.setattr(
        rag,
        "classify_intent",
        lambda query: {"intent": intent, "confidence": confidence, "reason": "test"},
    )
    monkeypatch.setattr(rag.rules_engine, "check", lambda query, intent: (False, "", ""))
    monkeypatch.setattr(rag, "should_extract_slots", lambda intent: False)


def test_answer_ambiguous_booking_asks_for_clarification_before_retrieval(monkeypatch):
    _patch_answer_basics(monkeypatch)
    monkeypatch.setattr(
        rag,
        "_classify_ambiguity_with_llm",
        lambda *args, **kwargs: "Anh/chi muon dat lich lai thu, dich vu hay tim dai ly?",
    )

    def fail_embed(query):
        raise AssertionError("LLM clarification should not reach retrieval")

    monkeypatch.setattr(rag, "embed_query", fail_embed)

    result = rag.answer("huong dan dat lich", session_id="clarify-booking")

    assert result["model_used"] == "clarification"
    assert "dat lich lai thu" in result["answer"]
    assert result["sources"] == []


def test_answer_specific_test_drive_procedure_still_uses_source(monkeypatch):
    docs = [
        {
            "score": 0.9,
            "content": (
                "Quy trinh dat lich lai thu Toyota gom 3 buoc. "
                "Buoc 1: chon mau xe muon lai thu. "
                "Buoc 2: chon dai ly va thoi gian. "
                "Buoc 3: xac nhan thong tin dat lich."
            ),
            "page": 1,
            "source": "basic.pdf",
            "source_id": "source",
            "document_category": DOCUMENT_CATEGORY_BASIC_ADVICE,
            "document_tags": ["lai_thu", "thu_tuc"],
            "chunk_id": "test-drive-procedure",
            "chunk_index": 0,
        }
    ]

    _patch_answer_basics(monkeypatch)
    monkeypatch.setattr(rag, "embed_query", lambda query: [0.0])
    monkeypatch.setattr(rag, "search", lambda *args, **kwargs: docs)
    monkeypatch.setattr(rag, "_lexical_support_docs", lambda *args, **kwargs: [])
    monkeypatch.setattr(rag, "_metadata_support_docs", lambda *args, **kwargs: [])
    monkeypatch.setattr(rag, "_rerank_retrieved_docs", lambda retrieved, **kwargs: retrieved)
    monkeypatch.setattr(rag, "_expand_with_neighbor_context", lambda retrieved, **kwargs: retrieved)
    monkeypatch.setattr(rag, "scroll_chunks", lambda **kwargs: docs)

    def fail_generate(messages):
        raise AssertionError("_generate should not be called for parsed procedures")

    monkeypatch.setattr(rag, "_generate", fail_generate)

    result = rag.answer("huong dan dat lich lai thu", session_id="test-drive-procedure")

    assert result["model_used"] == "deterministic_procedure"
    assert "lai thu" in result["answer"].lower()
    assert "1. chon mau xe" in result["answer"]


def test_answer_price_without_model_asks_for_clarification(monkeypatch):
    _patch_answer_basics(monkeypatch, intent="budget_filter")
    monkeypatch.setattr(
        rag,
        "_classify_ambiguity_with_llm",
        lambda *args, **kwargs: "Anh/chi muon xem gia mau xe nao, vi du Vios, Fortuner hay Wigo?",
    )

    def fail_embed(query):
        raise AssertionError("LLM clarification should not reach retrieval")

    monkeypatch.setattr(rag, "embed_query", fail_embed)

    result = rag.answer("gia xe bao nhieu", session_id="clarify-price")

    assert result["model_used"] == "clarification"
    assert "Vios" in result["answer"]
    assert result["sources"] == []


def test_answer_contextual_price_without_history_asks_for_clarification(monkeypatch):
    _patch_answer_basics(monkeypatch, intent="budget_filter")
    monkeypatch.setattr(
        rag,
        "_classify_ambiguity_with_llm",
        lambda *args, **kwargs: "Anh/chi dang muon hoi gia mau xe nao?",
    )
    monkeypatch.setattr(rag, "embed_query", lambda query: (_ for _ in ()).throw(
        AssertionError("LLM clarification should not reach retrieval")
    ))

    result = rag.answer("no co gia bao nhieu", session_id="clarify-context-price")

    assert result["model_used"] == "clarification"
    assert "mau xe" in result["answer"]


def test_answer_contextual_price_with_history_can_use_model(monkeypatch):
    docs = [
        {
            "score": 0.9,
            "content": (
                "Bảng giá khởi điểm tất cả xe Toyota tại Việt Nam:\n"
                "- Toyota Fortuner: SUV 7 chỗ - giá từ 1.055.000.000 VNĐ"
            ),
            "page": 1,
            "source": "basic.pdf",
            "source_id": "source",
            "document_category": DOCUMENT_CATEGORY_BASIC_ADVICE,
            "document_tags": ["gia_xe"],
            "mentioned_models": ["fortuner"],
            "chunk_id": "price-table",
            "chunk_index": 0,
        }
    ]

    _patch_answer_basics(monkeypatch, intent="budget_filter")
    state = rag.state_manager.get_or_create("context-price")
    state.add_turn("cho toi biet ve Fortuner", "Fortuner la mau SUV 7 cho.")
    monkeypatch.setattr(rag, "embed_query", lambda query: [0.0])
    monkeypatch.setattr(rag, "search", lambda *args, **kwargs: docs)
    monkeypatch.setattr(rag, "_lexical_support_docs", lambda *args, **kwargs: [])
    monkeypatch.setattr(rag, "_metadata_support_docs", lambda *args, **kwargs: [])
    monkeypatch.setattr(rag, "_rerank_retrieved_docs", lambda retrieved, **kwargs: retrieved)
    monkeypatch.setattr(rag, "_expand_with_neighbor_context", lambda retrieved, **kwargs: retrieved)
    monkeypatch.setattr(rag, "scroll_chunks", lambda **kwargs: docs)
    monkeypatch.setattr(rag, "_generate", lambda messages: (_ for _ in ()).throw(
        AssertionError("_generate should not be called for parsed prices")
    ))

    result = rag.answer("no co gia bao nhieu", session_id="context-price")

    assert result["model_used"] == "deterministic_price"
    assert "Toyota Fortuner" in result["answer"]


def test_answer_bare_advice_asks_for_clarification(monkeypatch):
    _patch_answer_basics(monkeypatch)
    monkeypatch.setattr(
        rag,
        "_classify_ambiguity_with_llm",
        lambda *args, **kwargs: "Anh/chi muon tu van theo ngan sach, so cho ngoi hay muc dich su dung?",
    )
    monkeypatch.setattr(rag, "embed_query", lambda query: (_ for _ in ()).throw(
        AssertionError("LLM clarification should not reach retrieval")
    ))

    result = rag.answer("tu van giup toi", session_id="clarify-advice")

    assert result["model_used"] == "clarification"
    assert "ngan sach" in result["answer"]


def test_answer_gray_area_query_can_use_llm_clarification(monkeypatch):
    _patch_answer_basics(monkeypatch)
    monkeypatch.setattr(
        rag,
        "_classify_ambiguity_with_llm",
        lambda *args, **kwargs: (
            "Anh/chi muon tu van theo ngan sach, so cho ngoi hay muc dich su dung?"
        ),
    )
    monkeypatch.setattr(rag, "embed_query", lambda query: (_ for _ in ()).throw(
        AssertionError("LLM clarification should not reach retrieval")
    ))

    result = rag.answer("co loai nao hop gia dinh khong", session_id="llm-clarify")

    assert result["model_used"] == "clarification"
    assert "ngan sach" in result["answer"]
    assert result["sources"] == []


def test_answer_empty_retrieval_asks_for_clarification(monkeypatch):
    _patch_answer_basics(monkeypatch, intent="car_info")
    monkeypatch.setattr(
        rag,
        "_classify_ambiguity_with_llm",
        lambda *args, **kwargs: (
            "Anh/chi muon hoi ve mau xe, gia xe hay dich vu Toyota nao?"
            if kwargs.get("retrieved") is not None
            else None
        ),
    )
    monkeypatch.setattr(rag, "embed_query", lambda query: [0.0])
    monkeypatch.setattr(rag, "search", lambda *args, **kwargs: [])
    monkeypatch.setattr(rag, "_lexical_support_docs", lambda *args, **kwargs: [])
    monkeypatch.setattr(rag, "_metadata_support_docs", lambda *args, **kwargs: [])
    monkeypatch.setattr(rag, "_rerank_retrieved_docs", lambda retrieved, **kwargs: retrieved)
    monkeypatch.setattr(rag, "_expand_with_neighbor_context", lambda retrieved, **kwargs: retrieved)
    monkeypatch.setattr(rag, "_generate", lambda messages: (_ for _ in ()).throw(
        AssertionError("_generate should not be called when retrieval is empty")
    ))

    result = rag.answer("thong tin ve Vios", session_id="empty-retrieval")

    assert result["model_used"] == "clarification"
    assert result["sources"] == []


def test_answer_low_relevance_retrieval_asks_for_clarification(monkeypatch):
    docs = [
        {
            "score": 0.2,
            "rerank_score": 0.2,
            "content": "Noi dung chung khong lien quan.",
            "page": 1,
            "source": "basic.pdf",
            "document_category": DOCUMENT_CATEGORY_BASIC_ADVICE,
            "chunk_id": "weak",
            "chunk_index": 0,
        }
    ]

    _patch_answer_basics(monkeypatch, intent="car_info")
    monkeypatch.setattr(
        rag,
        "_classify_ambiguity_with_llm",
        lambda *args, **kwargs: (
            "Anh/chi co the noi ro mau xe hoac dich vu can hoi khong?"
            if kwargs.get("retrieved") is not None
            else None
        ),
    )
    monkeypatch.setattr(rag, "embed_query", lambda query: [0.0])
    monkeypatch.setattr(rag, "search", lambda *args, **kwargs: docs)
    monkeypatch.setattr(rag, "_lexical_support_docs", lambda *args, **kwargs: [])
    monkeypatch.setattr(rag, "_metadata_support_docs", lambda *args, **kwargs: [])
    monkeypatch.setattr(rag, "_rerank_retrieved_docs", lambda retrieved, **kwargs: retrieved)
    monkeypatch.setattr(rag, "_expand_with_neighbor_context", lambda retrieved, **kwargs: retrieved)
    monkeypatch.setattr(rag, "_generate", lambda messages: (_ for _ in ()).throw(
        AssertionError("_generate should not be called for weak retrieval")
    ))

    result = rag.answer("thong tin ve Vios", session_id="weak-retrieval")

    assert result["model_used"] == "clarification"
    assert result["sources"] == []


def test_answer_repeated_procedure_query_is_deterministic_and_skips_llm(monkeypatch):
    docs = [
        {
            "score": 0.9,
            "content": (
                "Quy trinh dat lich bao duong Toyota gom 3 buoc. "
                "Buoc 1: dang nhap. Buoc 2: chon dich vu bao duong. Buoc 3: xac nhan."
            ),
            "page": 1,
            "source": "basic.pdf",
            "source_id": "source",
            "document_category": DOCUMENT_CATEGORY_BASIC_ADVICE,
            "document_tags": ["bao_duong", "thu_tuc"],
            "chunk_id": "procedure",
            "chunk_index": 0,
        }
    ]

    monkeypatch.setattr(rag, "state_manager", ConversationStateManager())
    monkeypatch.setattr(rag, "CHAT_LOG_SYNC", False)
    monkeypatch.setattr(
        rag,
        "classify_intent",
        lambda query: {"intent": "car_advice", "confidence": 0.9, "reason": "test"},
    )
    monkeypatch.setattr(rag.rules_engine, "check", lambda query, intent: (False, "", ""))
    monkeypatch.setattr(rag, "should_extract_slots", lambda intent: False)
    monkeypatch.setattr(rag, "embed_query", lambda query: [0.0])
    monkeypatch.setattr(rag, "search", lambda *args, **kwargs: docs)
    monkeypatch.setattr(rag, "_lexical_support_docs", lambda *args, **kwargs: [])
    monkeypatch.setattr(rag, "_metadata_support_docs", lambda *args, **kwargs: [])
    monkeypatch.setattr(rag, "_rerank_retrieved_docs", lambda retrieved, **kwargs: retrieved)
    monkeypatch.setattr(rag, "_expand_with_neighbor_context", lambda retrieved, **kwargs: retrieved)
    monkeypatch.setattr(rag, "scroll_chunks", lambda **kwargs: docs)

    def fail_generate(messages):
        raise AssertionError("_generate should not be called for parsed procedures")

    monkeypatch.setattr(rag, "_generate", fail_generate)

    first = rag.answer("huong dan dat lich bao duong", session_id="procedure-repeat")
    second = rag.answer("huong dan dat lich bao duong", session_id="procedure-repeat")

    assert first["model_used"] == "deterministic_procedure"
    assert second["model_used"] == "deterministic_procedure"
    assert first["answer"] == second["answer"]
    assert "1. dang nhap." in first["answer"]
    assert "3. xac nhan." in first["answer"]


def test_price_answer_from_basic_advice_pdf_finds_fortuner(monkeypatch):
    import fitz

    monkeypatch.setattr(rag, "scroll_chunks", lambda **kwargs: [])
    pdf = fitz.open("documents/file_tư_vấn_cơ_bản_updated.pdf")
    try:
        docs = [
            {
                "content": pdf[0].get_text("text"),
                "page": 1,
                "chunk_index": 0,
                "score": 0.9,
            }
        ]
    finally:
        pdf.close()

    result = _try_build_price_answer("giá xe fortuner", docs)

    assert result is not None
    answer_text, _ = result
    assert "Toyota Fortuner" in answer_text
    assert "1.055.000.000 VNĐ" in answer_text


def test_price_answer_finds_highest_price_from_table(monkeypatch):
    monkeypatch.setattr(rag, "scroll_chunks", lambda **kwargs: [])
    docs = [
        {
            "content": (
                "Bảng giá khởi điểm tất cả xe Toyota tại Việt Nam:\n"
                "• Toyota Fortuner: SUV 7 chỗ hạng D — giá từ 1.055.000.000 VNĐ\n"
                "• Toyota Alphard: MPV hạng sang — giá từ 4.415.000.000 VNĐ\n"
                "• Toyota Land Cruiser: SUV địa hình cao cấp — giá từ 4.580.000.000 VNĐ"
            ),
            "page": 1,
            "chunk_index": 0,
            "score": 0.9,
        }
    ]

    answer_text, _ = _try_build_price_answer("xe giá mắc nhất bạn đang có", docs)

    assert "Toyota Land Cruiser" in answer_text
    assert "4.580.000.000 VNĐ" in answer_text


def test_price_answer_uses_budget_against_price_table(monkeypatch):
    monkeypatch.setattr(rag, "scroll_chunks", lambda **kwargs: [])
    docs = [
        {
            "content": (
                "Bảng giá khởi điểm tất cả xe Toyota tại Việt Nam:\n"
                "• Toyota Land Cruiser Prado: SUV địa hình hạng sang — giá từ 3.460.000.000 VNĐ\n"
                "• Toyota Alphard: MPV hạng sang — giá từ 4.415.000.000 VNĐ\n"
                "• Toyota Land Cruiser: SUV địa hình cao cấp — giá từ 4.580.000.000 VNĐ"
            ),
            "page": 1,
            "chunk_index": 0,
            "score": 0.9,
        }
    ]

    answer_text, _ = _try_build_price_answer("tài chính tầm 10 tỷ mua xe gì", docs)

    assert "Với ngân sách khoảng 10.000.000.000 VNĐ" in answer_text
    assert "Toyota Land Cruiser" in answer_text
    assert "Toyota Alphard" in answer_text
    assert "Toyota Land Cruiser Prado" in answer_text


def test_answer_repeated_price_query_is_deterministic_and_skips_llm(monkeypatch):
    docs = [
        {
            "score": 0.9,
            "content": (
                "Bảng giá khởi điểm tất cả xe Toyota tại Việt Nam:\n"
                "• Toyota Fortuner: SUV 7 chỗ hạng D — giá từ 1.055.000.000 VNĐ\n"
                "• Toyota Land Cruiser: SUV địa hình cao cấp — giá từ 4.580.000.000 VNĐ"
            ),
            "page": 1,
            "source": "basic.pdf",
            "source_id": "source",
            "document_category": DOCUMENT_CATEGORY_BASIC_ADVICE,
            "document_tags": ["gia_xe"],
            "chunk_id": "price-table",
            "chunk_index": 0,
        }
    ]

    monkeypatch.setattr(rag, "state_manager", ConversationStateManager())
    monkeypatch.setattr(rag, "CHAT_LOG_SYNC", False)
    monkeypatch.setattr(
        rag,
        "classify_intent",
        lambda query: {"intent": "budget_filter", "confidence": 0.9, "reason": "test"},
    )
    monkeypatch.setattr(rag.rules_engine, "check", lambda query, intent: (False, "", ""))
    monkeypatch.setattr(rag, "should_extract_slots", lambda intent: False)
    monkeypatch.setattr(rag, "embed_query", lambda query: [0.0])
    monkeypatch.setattr(rag, "search", lambda *args, **kwargs: docs)
    monkeypatch.setattr(rag, "_lexical_support_docs", lambda *args, **kwargs: [])
    monkeypatch.setattr(rag, "_metadata_support_docs", lambda *args, **kwargs: [])
    monkeypatch.setattr(rag, "_rerank_retrieved_docs", lambda retrieved, **kwargs: retrieved)
    monkeypatch.setattr(rag, "_expand_with_neighbor_context", lambda retrieved, **kwargs: retrieved)
    monkeypatch.setattr(rag, "scroll_chunks", lambda **kwargs: docs)

    def fail_generate(messages):
        raise AssertionError("_generate should not be called for parsed prices")

    monkeypatch.setattr(rag, "_generate", fail_generate)

    first = rag.answer("giá xe fortuner", session_id="price-repeat")
    second = rag.answer("giá xe fortuner", session_id="price-repeat")

    assert first["model_used"] == "deterministic_price"
    assert second["model_used"] == "deterministic_price"
    assert first["answer"] == second["answer"]
    assert "Toyota Fortuner" in first["answer"]
    assert "1.055.000.000 VNĐ" in first["answer"]


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


def test_neighbor_expansion_keeps_cross_page_document_order(monkeypatch):
    def fake_search_neighbor_chunks(**kwargs):
        return [
            {
                "chunk_id": "page-9-chunk-0",
                "source_id": "source",
                "page": 9,
                "chunk_index": 0,
                "content": "Buoc 4: chon dai ly. Buoc 5: chon gio. Buoc 6: xac nhan.",
                "score": 0.0,
            }
        ]

    monkeypatch.setattr(rag, "search_neighbor_chunks", fake_search_neighbor_chunks)
    docs = [
        {
            "chunk_id": "page-8-chunk-5",
            "source_id": "source",
            "page": 8,
            "chunk_index": 5,
            "content": "Quy trinh gom 6 buoc. Buoc 1: dang nhap. Buoc 2: chon xe. Buoc 3: chon dich vu.",
            "score": 0.9,
        }
    ]

    expanded = _expand_with_neighbor_context(docs, seed_limit=1)

    assert [doc["chunk_id"] for doc in expanded] == [
        "page-8-chunk-5",
        "page-9-chunk-0",
    ]


def test_neighbor_expansion_can_use_wider_procedure_window(monkeypatch):
    calls = []

    def fake_search_neighbor_chunks(**kwargs):
        calls.append(kwargs)
        return [
            {
                "chunk_id": "page-9-chunk-0",
                "source_id": "source",
                "page": 9,
                "chunk_index": 0,
                "content": "Buoc 4: chon dai ly.",
                "score": 0.0,
            },
            {
                "chunk_id": "page-9-chunk-1",
                "source_id": "source",
                "page": 9,
                "chunk_index": 1,
                "content": "Buoc 5: chon gio. Buoc 6: xac nhan.",
                "score": 0.0,
            },
        ]

    monkeypatch.setattr(rag, "search_neighbor_chunks", fake_search_neighbor_chunks)
    docs = [
        {
            "chunk_id": "page-8-chunk-5",
            "source_id": "source",
            "page": 8,
            "chunk_index": 5,
            "content": "Quy trinh gom 6 buoc. Buoc 1: dang nhap. Buoc 2: chon xe. Buoc 3: chon dich vu.",
            "score": 0.9,
        }
    ]

    expanded = _expand_with_neighbor_context(
        docs,
        seed_limit=1,
        neighbor_window=3,
    )

    assert calls[0]["window"] == 3
    assert [doc["chunk_id"] for doc in expanded] == [
        "page-8-chunk-5",
        "page-9-chunk-0",
        "page-9-chunk-1",
    ]


