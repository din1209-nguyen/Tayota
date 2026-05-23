"""
rag.py  —  RAG pipeline đầy đủ
Flow:
    query
      ↓ Intent Classifier
      ↓ Business Rules
      ↓ Slot Extractor  (cập nhật state)
      ↓ Conversation State Manager
      ↓ Smart Car Consultant  (quyết định prompt + skip_rag)
      ↓ [RAG Retrieve nếu cần]
      ↓ LLM Generate  (Groq → Ollama → Gemini)
      ↓ Compose final response

LLM_PROVIDER (trong .env):
    groq    → Groq API (default)
    ollama  → Ollama local (gemma3:4b hoặc model bất kỳ)
    gemini  → Google Gemini
    auto    → Groq → fallback Ollama → fallback Gemini
"""

import os
import httpx
import re
import unicodedata
from typing import List, Dict, Any

from groq import Groq

from embed import embed_query
from vector_database import search
from intent_classifier import classify_intent
from business_rules import rules_engine
from slot_extractor import empty_slots, extract_slots, should_extract_slots
from conversation_state_manager import state_manager, ConversationState
from logic_smart_car_consultant import smart_consultant

from dotenv import load_dotenv

load_dotenv()

# ── Cấu hình ──────────────────────────────────────────────────────────────────
GROQ_API_KEY = os.getenv("GROQ_API_KEY", "")
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")

# Chọn provider: groq | ollama | gemini | auto
LLM_PROVIDER = os.getenv("LLM_PROVIDER", "groq").split("#", 1)[0].strip().lower()

GROQ_MODEL = os.getenv("GROQ_MODEL", "llama-3.3-70b-versatile")
GEMINI_MODEL = os.getenv("GEMINI_MODEL", "gemini-2.0-flash")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "gemma4:e2b")
OLLAMA_URL = os.getenv("OLLAMA_URL", "http://localhost:11434/api/chat")

TOP_K = 5

BASIC_ADVICE_DOCUMENT_SOURCE = "file tư vấn cơ bản.pdf"
GENERAL_DOCUMENT_SOURCES = [BASIC_ADVICE_DOCUMENT_SOURCE]
VEHICLE_DOCUMENT_SOURCES = [
    "Toyota HILUX .pdf",
    "TOYOTA SEDAN.pdf",
    "TOYOTA SUV.pdf",
    "TOYOTA WIGO.pdf",
    "TOYOTA ĐA DỤNG.pdf",
]
TOYOTA_MODEL_KEYWORDS = [
    "alphard",
    "altis",
    "avanza",
    "avanza premio",
    "camry",
    "corolla",
    "corolla altis",
    "corolla cross",
    "cross",
    "fortuner",
    "hilux",
    "innova",
    "innova cross",
    "land cruiser",
    "land cruiser prado",
    "prado",
    "raize",
    "veloz",
    "veloz cross",
    "vios",
    "wigo",
    "yaris",
    "yaris cross",
]
OFFROAD_KEYWORDS = [
    "off-road",
    "offroad",
    "địa hình",
    "leo núi",
    "gồ ghề",
    "đường xấu",
    "đồi núi",
    "rừng",
    "bùn",
    "cát",
    "sỏi",
]
OFFROAD_PREFERRED_MODELS = [
    "hilux",
    "fortuner",
    "land cruiser",
    "prado",
]
OFFROAD_DOWNRANK_MODELS = [
    "innova",
    "veloz",
    "yaris cross",
    "raize",
    "wigo",
    "vios",
    "camry",
    "corolla",
]
OFFROAD_FEATURE_TERMS = [
    "4x4",
    "awd",
    "off-road",
    "multi terrain",
    "terrain select",
    "khóa vi sai",
    "vi sai",
    "hỗ trợ đổ đèo",
    "dac",
    "gầm cao",
    "địa hình",
    "đường xấu",
]

# Validate API keys theo provider
if LLM_PROVIDER in ("groq", "auto") and not GROQ_API_KEY:
    print("⚠️  GROQ_API_KEY chưa set — Groq sẽ không dùng được")
if LLM_PROVIDER in ("gemini",) and not GEMINI_API_KEY:
    raise EnvironmentError("❌ Thiếu GEMINI_API_KEY trong .env")

groq_client = Groq(api_key=GROQ_API_KEY) if GROQ_API_KEY else None

SYSTEM_PROMPT = """
Bạn là trợ lý tư vấn mua xe chuyên nghiệp của TOYOTA Việt Nam.

Nguyên tắc chung:
- Luôn trả lời bằng tiếng Việt, giọng văn chuyên nghiệp, thân thiện.
- Chỉ dùng thông tin có trong dữ liệu tham khảo và ngữ cảnh hội thoại được cung cấp.
- Không bịa giá, thông số, phiên bản, trang bị hoặc khuyến mãi.
- Nếu dữ liệu không đủ, nói rõ phần nào chưa có thông tin thay vì suy đoán.
- Không tự đặt câu hỏi follow-up ở cuối câu trả lời; hệ thống sẽ xử lý việc hỏi thêm.

Phạm vi dữ liệu tham khảo:
- Hệ thống đã tự lọc nguồn trước khi gửi dữ liệu cho bạn.
- Nếu người dùng hỏi thông tin chung, danh mục xe, hãng có những xe/dòng xe nào, quy trình mua xe, đặt lịch lái thử, bảo dưỡng hoặc kinh nghiệm lựa chọn xe mà không nhắc một mẫu xe/dòng xe cụ thể, dữ liệu tham khảo sẽ đến từ tài liệu tư vấn cơ bản.
- Chỉ khi người dùng hỏi về một mẫu xe cụ thể hoặc một dòng xe cụ thể như Vios, Wigo, Hilux, Sedan, SUV, MPV/đa dụng, bán tải hoặc Hatchback, dữ liệu tham khảo mới đến từ nhóm tài liệu xe. Hãy ưu tiên trả lời đúng xe/dòng xe được hỏi, không dùng kiến thức chung để bù vào giá, thông số, phiên bản hoặc trang bị chưa có trong nguồn.
- Nếu dữ liệu tham khảo không đúng hoặc không đủ cho câu hỏi, hãy nói rõ chưa có thông tin trong dữ liệu hiện tại.

Cách trả lời theo loại câu hỏi:
- Nếu người dùng hỏi thông tin chung về danh mục xe, mua xe, sử dụng xe, bảo dưỡng, tài chính, an toàn hoặc kinh nghiệm lựa chọn xe, hãy trả lời bằng thông tin chung có trong nguồn.
- Nếu người dùng hỏi thông tin về một mẫu xe cụ thể, hãy giới thiệu trực tiếp mẫu xe đó: tổng quan, phiên bản/giá/thông số/trang bị nếu có trong dữ liệu.
- Nếu người dùng hỏi tư vấn chọn xe theo nhu cầu nhưng chưa nêu mẫu xe cụ thể, hãy dùng dữ liệu tư vấn chung và nhu cầu đã biết để định hướng lựa chọn; chỉ nêu mẫu xe cụ thể khi nguồn tham khảo có căn cứ rõ ràng.
- Nếu người dùng hỏi so sánh, chỉ so sánh theo các mẫu xe/tiêu chí có trong dữ liệu tham khảo.
- Nếu người dùng hỏi danh sách xe, hãy liệt kê đúng các xe có trong dữ liệu tham khảo.

Khi tư vấn chọn xe, ưu tiên cấu trúc:
1. Nhận định nhu cầu
2. Gợi ý xe phù hợp
3. Lý do chọn
4. Lưu ý hoặc lựa chọn thêm nếu có
Nếu không phải tư vấn chọn xe thì không cần tuân theo cấu trúc này, chỉ cần trả lời trực tiếp theo thông tin có trong dữ liệu tham khảo là được.

Không áp dụng cấu trúc tư vấn trên cho câu hỏi chỉ yêu cầu giới thiệu, tra cứu thông tin, liệt kê hoặc so sánh.
""".strip()


def _ollama_base_url() -> str:
    base_url = OLLAMA_URL.rstrip("/")
    for suffix in ("/api/chat", "/api/generate"):
        if base_url.endswith(suffix):
            return base_url[: -len(suffix)]
    return base_url


def _resolve_ollama_model(preferred_model: str) -> str:
    """Return the preferred Ollama model if installed, otherwise a local fallback."""
    try:
        resp = httpx.get(f"{_ollama_base_url()}/api/tags", timeout=10)
        resp.raise_for_status()
        data = resp.json()
    except Exception:
        return preferred_model

    models = [
        m.get("name")
        for m in data.get("models", [])
        if isinstance(m, dict) and m.get("name")
    ]
    if not models:
        return preferred_model

    if preferred_model in models:
        return preferred_model

    fallback_model = models[0]
    print(
        f"⚠️ Ollama model '{preferred_model}' not found, using '{fallback_model}' instead"
    )
    return fallback_model


# ── LLM helpers ───────────────────────────────────────────────────────────────


def _ask_groq(messages: List[Dict]) -> str:
    if not groq_client:
        raise RuntimeError("Groq client chưa được khởi tạo (thiếu API key)")
    resp = groq_client.chat.completions.create(
        model=GROQ_MODEL,
        messages=messages,
        temperature=0.2,
        max_tokens=1024,
    )
    return resp.choices[0].message.content


def _ask_ollama(messages: List[Dict]) -> str:
    """
    Gọi Ollama local qua REST API.
    Ollama nhận messages theo chuẩn OpenAI (role + content).
    System prompt được inject vào đầu messages nếu chưa có.
    """
    # Đảm bảo có system message
    if not messages or messages[0]["role"] != "system":
        full_messages = [{"role": "system", "content": SYSTEM_PROMPT}] + messages
    else:
        full_messages = messages

    model_name = _resolve_ollama_model(OLLAMA_MODEL)

    payload = {
        "model": model_name,
        "messages": full_messages,
        "stream": False,
        "options": {
            "temperature": 0.2,
            "num_predict": 1024,
            "num_ctx": 4096,  # giới hạn context để tiết kiệm RAM
        },
    }
    try:
        resp = httpx.post(OLLAMA_URL, json=payload, timeout=120)
        resp.raise_for_status()
    except httpx.HTTPStatusError as e:
        status = None
        try:
            status = e.response.status_code if e.response is not None else None
        except Exception:
            status = None
        if status == 404:
            # Try common alternate endpoint (/api/generate)
            alt_url = f"{_ollama_base_url()}/api/generate"
            print(f"⚠️ Ollama endpoint returned 404, retrying {alt_url}")
            resp = httpx.post(alt_url, json=payload, timeout=120)
            resp.raise_for_status()
        else:
            body = ""
            try:
                body = e.response.text.strip() if e.response is not None else ""
            except Exception:
                body = ""
            detail = body or str(e)
            raise RuntimeError(f"Ollama HTTP {status}: {detail}") from e

    # Parse known response shapes: /api/chat -> {"message": {"content": ...}}
    # /api/generate  -> {"text": "..."} or similar. Fall back to raw text.
    j = None
    try:
        j = resp.json()
    except Exception:
        return resp.text

    if isinstance(j, dict):
        if (
            "message" in j
            and isinstance(j["message"], dict)
            and "content" in j["message"]
        ):
            return j["message"]["content"]
        if "text" in j:
            return j["text"]

    return resp.text


def _ask_gemini(messages: List[Dict]) -> str:
    try:
        from google import genai
        from google.genai import types
    except Exception as exc:
        raise RuntimeError("Gemini provider requires the google-genai package.") from exc

    client = genai.Client(api_key=GEMINI_API_KEY)
    prompt = "\n\n".join(
        f"{'User' if m['role'] == 'user' else 'Assistant'}: {m['content']}"
        for m in messages
        if m["role"] != "system"
    )
    resp = client.models.generate_content(
        model=GEMINI_MODEL,
        contents=f"{SYSTEM_PROMPT}\n\n{prompt}",
        config=types.GenerateContentConfig(temperature=0.2, max_output_tokens=1024),
    )
    return resp.text


def _generate(messages: List[Dict]) -> tuple[str, str]:
    """
    Gọi LLM theo LLM_PROVIDER.

    Provider modes:
        groq   → chỉ Groq, không fallback
        ollama → chỉ Ollama local, không fallback
        gemini → chỉ Gemini, không fallback
        auto   → Groq → Ollama → Gemini (fallback theo thứ tự)
    """
    if LLM_PROVIDER == "ollama":
        try:
            return _ask_ollama(messages), OLLAMA_MODEL
        except Exception as e:
            print(f"⚠️  Ollama lỗi ({e}), thử provider dự phòng...")
            if GROQ_API_KEY:
                try:
                    return _ask_groq(messages), GROQ_MODEL
                except Exception as groq_error:
                    print(f"⚠️  Groq lỗi ({groq_error}), thử Gemini...")
            if GEMINI_API_KEY:
                return _ask_gemini(messages), GEMINI_MODEL
            raise RuntimeError(
                "Ollama không khả dụng và không có provider dự phòng hợp lệ (GROQ_API_KEY/GEMINI_API_KEY)."
            ) from e

    if LLM_PROVIDER == "groq":
        return _ask_groq(messages), GROQ_MODEL

    if LLM_PROVIDER == "gemini":
        return _ask_gemini(messages), GEMINI_MODEL

    # auto — fallback chain
    if GROQ_API_KEY:
        try:
            return _ask_groq(messages), GROQ_MODEL
        except Exception as e:
            print(f"⚠️  Groq lỗi ({e}), fallback Ollama...")

    try:
        return _ask_ollama(messages), OLLAMA_MODEL
    except Exception as e:
        print(f"⚠️  Ollama lỗi ({e}), fallback Gemini...")

    return _ask_gemini(messages), GEMINI_MODEL


def _build_context(retrieved: List[Dict[str, Any]]) -> str:
    parts = []
    for i, r in enumerate(retrieved, 1):
        parts.append(
            f"Nguồn {i}:\n"
            f"Tài liệu: {r.get('source') or 'không rõ'} | "
            f"Trang: {r['page']} | Độ tin cậy: {r['score']:.2f}\n\n"
            f"{r['content']}"
        )
    return "\n\n==========\n\n".join(parts)


def _needs_history_for_retrieval(query: str) -> bool:
    q = query.lower()
    toyota_names = [
        "fortuner",
        "innova",
        "vios",
        "wigo",
        "hilux",
        "corolla",
        "camry",
        "yaris",
        "veloz",
        "avanza",
        "raize",
        "cross",
        "land cruiser",
        "alphard",
    ]
    if any(name in q for name in toyota_names):
        return False
    contextual_markers = [
        "n\u00f3",
        "xe n\u00e0y",
        "m\u1eabu n\u00e0y",
        "d\u00f2ng n\u00e0y",
        "v\u1eady",
        "th\u00f4i",
        "c\u00f2n",
        "gia \u0111\u00ecnh t\u00f4i",
        "nh\u00e0 t\u00f4i",
    ]
    return len(query.split()) <= 8 or any(marker in q for marker in contextual_markers)


def _build_retrieval_query(
    query: str,
    state: ConversationState,
    include_slots: bool,
) -> str:
    parts = [f"Cau hoi hien tai: {query}"]

    if _needs_history_for_retrieval(query):
        previous_user_turns = [
            msg["content"]
            for msg in state.get_recent_history(n_turns=2)
            if msg.get("role") == "user"
        ]
        if previous_user_turns:
            parts.append("Ngu canh hoi thoai gan day:")
            parts.extend(f"- {turn}" for turn in previous_user_turns)

    if include_slots:
        filled_slots = state.get_filled_slots()
        if filled_slots:
            slot_text = ", ".join(f"{k}: {v}" for k, v in filled_slots.items())
            parts.append(f"Nhu cau da biet: {slot_text}")

    return "\n".join(parts)


def _normalize_lookup_text(text: str) -> str:
    normalized = unicodedata.normalize("NFD", text.lower().replace("đ", "d"))
    normalized = "".join(
        char for char in normalized if unicodedata.category(char) != "Mn"
    )
    return re.sub(r"\s+", " ", normalized).strip()


def _contains_lookup_keyword(normalized_text: str, keyword: str) -> bool:
    normalized_keyword = _normalize_lookup_text(keyword)
    pattern = rf"(?<![a-z0-9]){re.escape(normalized_keyword)}(?![a-z0-9])"
    return re.search(pattern, normalized_text) is not None


def _mentions_vehicle_detail(query: str, state: ConversationState) -> bool:
    normalized_query = _normalize_lookup_text(query)
    if _contains_vehicle_keyword(normalized_query):
        return True

    if _is_general_catalog_query(query):
        return False

    contextual_markers = [
        "no",
        "xe nay",
        "mau nay",
        "dong nay",
        "ban nay",
        "phien ban nay",
    ]
    if not any(marker in normalized_query for marker in contextual_markers):
        return False

    previous_user_turns = [
        msg["content"]
        for msg in state.get_recent_history(n_turns=2)
        if msg.get("role") == "user"
    ]
    normalized_history = _normalize_lookup_text("\n".join(previous_user_turns))
    return _contains_vehicle_keyword(normalized_history)


def _contains_vehicle_keyword(normalized_text: str) -> bool:
    if any(
        _contains_lookup_keyword(normalized_text, keyword)
        for keyword in TOYOTA_MODEL_KEYWORDS
    ):
        return True

    simple_categories = ["hatchback", "mpv", "pickup", "sedan", "suv"]
    if any(
        _contains_lookup_keyword(normalized_text, keyword)
        for keyword in simple_categories
    ):
        return True

    contextual_category_patterns = [
        r"(?<![a-z0-9])(?:dong|xe|mau|loai)\s+ban\s+tai(?![a-z0-9])",
        r"(?<![a-z0-9])ban\s+tai\s+toyota(?![a-z0-9])",
        r"(?<![a-z0-9])(?:dong|xe|mau|loai)\s+da\s+dung(?![a-z0-9])",
        r"(?<![a-z0-9])da\s+dung\s+toyota(?![a-z0-9])",
    ]
    return any(
        re.search(pattern, normalized_text) is not None
        for pattern in contextual_category_patterns
    )


def _is_general_catalog_query(query: str) -> bool:
    normalized_text = _normalize_lookup_text(query)
    catalog_markers = [
        "danh muc xe",
        "cac dong xe",
        "nhung dong xe",
        "dong xe nao",
        "co bao nhieu dong xe",
        "co nhung dong xe",
        "cac dong xe co nhung xe nao",
        "cac xe nao",
        "nhung xe nao",
        "co nhung xe nao",
        "danh sach xe",
        "hang dang co",
        "ben hang dang co",
    ]
    return any(marker in normalized_text for marker in catalog_markers)


def _document_scope_for_query(
    query: str,
    state: ConversationState,
) -> tuple[str, List[str]]:
    normalized_query = _normalize_lookup_text(query)
    if _is_general_catalog_query(query) and not _contains_vehicle_keyword(normalized_query):
        return "general", GENERAL_DOCUMENT_SOURCES
    if _mentions_vehicle_detail(query, state):
        return "vehicle", VEHICLE_DOCUMENT_SOURCES
    return "general", GENERAL_DOCUMENT_SOURCES


def _is_offroad_need(query: str, state: ConversationState) -> bool:
    q = query.lower()
    if any(keyword in q for keyword in OFFROAD_KEYWORDS):
        return True
    slots = state.get_filled_slots()
    return slots.get("region") == "địa hình" or slots.get("purpose") == "off-road"


def _expand_retrieval_query_for_domain(query: str, offroad_need: bool) -> str:
    if not offroad_need:
        return query
    expansion = (
        "Nhu cầu địa hình off-road leo núi đường xấu gồ ghề. "
        "Ưu tiên Toyota Hilux, Fortuner, Land Cruiser, Land Cruiser Prado, "
        "4x4, gầm cao, hỗ trợ đổ đèo, khóa vi sai, Multi Terrain Select. "
        "Không ưu tiên xe đô thị hoặc MPV nếu không có năng lực địa hình rõ ràng."
    )
    return f"{query}\n{expansion}"


def _rerank_retrieved_docs(
    retrieved: List[Dict[str, Any]],
    *,
    offroad_need: bool,
    limit: int,
) -> List[Dict[str, Any]]:
    if not offroad_need:
        return retrieved[:limit]

    ranked = []
    for doc in retrieved:
        text = f"{doc.get('source') or ''}\n{doc.get('content') or ''}".lower()
        score = float(doc.get("score") or 0)

        if any(model in text for model in OFFROAD_PREFERRED_MODELS):
            score += 0.35
        if any(term in text for term in OFFROAD_FEATURE_TERMS):
            score += 0.25
        if any(model in text for model in OFFROAD_DOWNRANK_MODELS):
            score -= 0.25
            if not any(term in text for term in OFFROAD_FEATURE_TERMS):
                score -= 0.25

        adjusted = dict(doc)
        adjusted["rerank_score"] = score
        ranked.append(adjusted)

    ranked.sort(key=lambda item: item.get("rerank_score", item.get("score", 0)), reverse=True)
    return ranked[:limit]


def _domain_answer_instruction(offroad_need: bool) -> str:
    if not offroad_need:
        return ""
    return (
        "Lưu ý miền nghiệp vụ: người dùng đang hỏi nhu cầu địa hình/off-road. "
        "Chỉ đề xuất xe khi nguồn truy xuất có bằng chứng rõ về năng lực địa hình "
        "như 4x4, gầm cao, hỗ trợ đổ đèo, khóa vi sai, Multi Terrain Select, "
        "hoặc nội dung off-road. Không đề xuất xe đô thị/MPV chỉ vì có chữ Cross "
        "hoặc vì là xe rộng nếu không có bằng chứng địa hình rõ."
    )


# ── Pipeline chính ────────────────────────────────────────────────────────────


def answer(
    query: str,
    session_id: str = "default",
) -> Dict[str, Any]:

    # ── Lấy / tạo conversation state ─────────────────────────────────────────
    state: ConversationState = state_manager.get_or_create(session_id)

    # ── Bước 1: Classify intent ───────────────────────────────────────────────
    intent_result = classify_intent(query)
    intent = intent_result["intent"]
    print(f"🔍 Intent: {intent} | confidence: {intent_result.get('confidence', '?')}")

    # ── Bước 2: Business rules ────────────────────────────────────────────────
    blocked, rule_name, rule_response = rules_engine.check(query, intent_result)

    if blocked:
        print(f"🚫 Blocked: {rule_name}")
        state.add_turn(query, rule_response)
        return _make_result(
            answer=rule_response,
            sources=[],
            model_used="business_rules",
            intent=intent,
            rule_name=rule_name,
            state=state,
            session_id=session_id,
        )

    warning_prefix = f"{rule_response}\n\n---\n\n" if rule_response else ""

    # ── Bước 3: Slot extraction ───────────────────────────────────────────────
    use_slot_context = should_extract_slots(intent)
    if use_slot_context:
        new_slots = extract_slots(query)
        state.update_slots(new_slots)
    else:
        new_slots = empty_slots()
        print(f"Slot extraction skipped for intent: {intent}")
    state.update_stage(intent)
    print(f"📦 Slots filled: {state.get_filled_slots()}")
    print(f"📋 Stage: {state.stage}")

    # ── Bước 4: Smart consultant (sơ bộ, chưa có RAG context) ────────────────
    prelim_decision = smart_consultant.decide(query, state, rag_context="")

    # ── Bước 5: RAG retrieve ──────────────────────────────────────────────────
    sources = []
    rag_context = ""
    offroad_need = _is_offroad_need(query, state)
    document_scope, source_names = _document_scope_for_query(query, state)

    # Bước 5: RAG retrieve


    if not prelim_decision.skip_rag:
        # Tăng top_k khi hỏi liệt kê
        list_keywords = ["tất cả", "những xe", "có những", "danh sách", "các xe", "nêu"]
        dynamic_top_k = 15 if any(kw in query.lower() for kw in list_keywords) else TOP_K
        retrieval_top_k = 20 if offroad_need else dynamic_top_k

        retrieval_query = _build_retrieval_query(
            query,
            state,
            include_slots=use_slot_context,
        )
        retrieval_query = _expand_retrieval_query_for_domain(
            retrieval_query,
            offroad_need=offroad_need,
        )
        print(
            f"Document scope: {document_scope} | sources: {', '.join(source_names)}"
        )
        query_vec = embed_query(retrieval_query)
        retrieved = search(
            query_vec,
            top_k=retrieval_top_k,
            score_threshold=0.35,
            source_names=source_names,
        )
        if not retrieved:
            retrieved = search(
                query_vec,
                top_k=retrieval_top_k,
                score_threshold=0.2,
                source_names=source_names,
            )
        retrieved = _rerank_retrieved_docs(
            retrieved,
            offroad_need=offroad_need,
            limit=dynamic_top_k,
        )

        if retrieved:
            rag_context = _build_context(retrieved)
            sources = [
                {
                    "source": r.get("source"),
                    "page": r["page"],
                    "score": round(r["score"], 3),
                }
                for r in retrieved
            ]
        else:
            no_data = "Tôi chưa có thông tin chính xác về nội dung này trong dữ liệu hiện tại."
            final_msg = smart_consultant.compose_final_response(
                no_data, prelim_decision
            )
            final_msg = warning_prefix + final_msg
            state.add_turn(query, final_msg)
            return _make_result(
                answer=final_msg,
                sources=[],
                model_used="none",
                intent=intent,
                rule_name=rule_name,
                state=state,
                session_id=session_id,
            )

    # ── Bước 6: Build prompt cuối với RAG context đầy đủ ─────────────────────
    final_decision = smart_consultant.decide(query, state, rag_context=rag_context)
    domain_instruction = _domain_answer_instruction(offroad_need)

    messages = [
        {"role": "system", "content": SYSTEM_PROMPT},
        *state.get_recent_history(n_turns=3),
        {
            "role": "user",
            "content": (
                f"Dữ liệu tham khảo:\n{rag_context}\n\n"
                f"---\n\n"
                f"{domain_instruction}\n\n"
                f"---\n\n"
                f"Câu hỏi người dùng:\n{final_decision.prompt}"
            ),
        },
    ]

    # ── Bước 7: Generate ──────────────────────────────────────────────────────
    llm_response, model_used = _generate(messages)

    final_answer = warning_prefix + smart_consultant.compose_final_response(
        llm_response, final_decision
    )

    state.add_turn(query, final_answer)

    return _make_result(
        answer=final_answer,
        sources=sources,
        model_used=model_used,
        intent=intent,
        rule_name=rule_name,
        state=state,
        session_id=session_id,
    )


def _make_result(
    answer: str,
    sources: list,
    model_used: str,
    intent: str,
    rule_name: str,
    state: ConversationState,
    session_id: str,
) -> Dict[str, Any]:
    if hasattr(state_manager, "save"):
        state_manager.save(state)

    return {
        "answer": answer,
        "sources": sources,
        "model_used": model_used,
        "intent": intent,
        "rule_triggered": rule_name,
        "stage": state.stage,
        "slots": state.get_filled_slots(),
        "session_id": session_id,
    }


# ── CLI loop ──────────────────────────────────────────────────────────────────
if __name__ == "__main__":
    import uuid

    SESSION_ID = str(uuid.uuid4())
    print(f"🤖 Toyota RAG Chatbot  |  session: {SESSION_ID}")
    print(f"🧠 LLM provider: {LLM_PROVIDER.upper()}", end="")
    if LLM_PROVIDER == "ollama":
        print(f"  (model: {OLLAMA_MODEL})")
    elif LLM_PROVIDER == "groq":
        print(f"  (model: {GROQ_MODEL})")
    elif LLM_PROVIDER == "gemini":
        print(f"  (model: {GEMINI_MODEL})")
    else:
        print(f"  (Groq → Ollama:{OLLAMA_MODEL} → Gemini)")
    print("Lệnh đặc biệt: 'reset' | 'state' | 'exit'\n")

    while True:
        query = input("Bạn: ").strip()
        if not query:
            continue
        if query.lower() in ("exit", "quit", "thoát"):
            break
        if query.lower() == "reset":
            state_manager.reset(SESSION_ID)
            print("🔄 Đã reset hội thoại.\n")
            continue
        if query.lower() == "state":
            s = state_manager.get(SESSION_ID)
            print(f"📊 {s.summary() if s else 'Không có state'}\n")
            continue

        result = answer(query, session_id=SESSION_ID)
        print(
            f"\n🤖 ({result['model_used']}) [intent={result['intent']} | stage={result['stage']}]:"
        )
        print(result["answer"])
        print(f"\n📦 Slots : {result['slots']}")
        print(f"📚 Nguồn : {result['sources']}")
        print(f"🔒 Rule  : {result['rule_triggered']}\n")
        print("-" * 60)
