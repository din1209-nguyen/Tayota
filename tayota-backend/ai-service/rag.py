"""
rag.py - Toyota RAG pipeline.

Flow:
    query
      -> Intent Classifier
      -> Business Rules
      -> Slot Extractor
      -> Conversation State Manager
      -> Smart Car Consultant
      -> RAG Retrieve when needed
      -> LLM Generate with Groq
      -> Compose final response
"""

import os
import re
import unicodedata
from typing import List, Dict, Any

from groq import Groq

from embed import embed_query
from vector_database import (
    DOCUMENT_CATEGORY_BASIC_ADVICE,
    DOCUMENT_CATEGORY_HILUX,
    DOCUMENT_CATEGORY_MPV,
    DOCUMENT_CATEGORY_SEDAN,
    DOCUMENT_CATEGORY_SUMMARY,
    DOCUMENT_CATEGORY_SUV,
    DOCUMENT_CATEGORY_WIGO,
    infer_document_category,
    search,
    scroll_chunks,
    search_neighbor_chunks,
)
from intent_classifier import classify_intent
from business_rules import rules_engine
from slot_extractor import extract_slots, should_extract_slots
from conversation_state_manager import state_manager, ConversationState
from response_links import append_relevant_links
from logic_smart_car_consultant import smart_consultant

from dotenv import load_dotenv

load_dotenv()

# ── Cấu hình ──────────────────────────────────────────────────────────────────
GROQ_API_KEY = os.getenv("GROQ_API_KEY", "")

# Only Groq is supported for answer generation.
LLM_PROVIDER = "groq"

GROQ_MODEL = os.getenv("GROQ_MODEL", "llama-3.3-70b-versatile")

TOP_K = 5
RETRIEVAL_CANDIDATES_TOP_K = 30
LIST_CONTEXT_TOP_K = 15
MAX_CONTEXT_CHUNKS = 12
MAX_CONTEXT_CHARS = 10000

GENERAL_DOCUMENT_CATEGORIES = [
    DOCUMENT_CATEGORY_BASIC_ADVICE,
    DOCUMENT_CATEGORY_SUMMARY,
]
VEHICLE_DOCUMENT_CATEGORIES = [
    DOCUMENT_CATEGORY_HILUX,
    DOCUMENT_CATEGORY_SEDAN,
    DOCUMENT_CATEGORY_SUV,
    DOCUMENT_CATEGORY_WIGO,
    DOCUMENT_CATEGORY_MPV,
]
HYBRID_DOCUMENT_CATEGORIES = [
    *GENERAL_DOCUMENT_CATEGORIES,
    *VEHICLE_DOCUMENT_CATEGORIES,
]
LEGACY_DOCUMENT_SOURCES_BY_CATEGORY = {
    DOCUMENT_CATEGORY_BASIC_ADVICE: ["file tư vấn cơ bản.pdf"],
    DOCUMENT_CATEGORY_SUMMARY: ["summary"],
    DOCUMENT_CATEGORY_HILUX: ["Toyota HILUX .pdf"],
    DOCUMENT_CATEGORY_SEDAN: ["TOYOTA SEDAN.pdf"],
    DOCUMENT_CATEGORY_SUV: ["TOYOTA SUV.pdf"],
    DOCUMENT_CATEGORY_WIGO: ["TOYOTA WIGO.pdf"],
    DOCUMENT_CATEGORY_MPV: ["TOYOTA ĐA DỤNG.pdf"],
}
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
SPECIFIC_MODEL_KEYWORDS = [
    keyword
    for keyword in TOYOTA_MODEL_KEYWORDS
    if len(keyword.split()) > 1 or keyword not in {"cross"}
]
OVERVIEW_QUERY_MARKERS = [
    "gioi thieu",
    "thong tin",
    "tong quan",
    "chi tiet",
    "cho toi biet",
]
GENERAL_INFORMATION_QUERY_MARKERS = [
    "thong tin chung",
    "thong tin co ban",
    "tu van co ban",
    "quy trinh",
    "thu tuc",
    "ho so",
    "giay to",
    "dat lich",
    "lai thu",
    "bao duong",
    "bao hanh",
    "tra gop",
    "vay mua xe",
    "tai chinh",
    "dang ky",
    "dang ki",
    "lien he",
    "showroom",
    "dai ly",
    "khuyen mai",
    "uu dai",
    "cham soc khach hang",
    "kinh nghiem",
    "luu y",
    "an toan",
]
HIGH_VALUE_VEHICLE_TERMS = [
    "gia",
    "gia khoi diem",
    "phien ban",
    "tong quan",
    "thong tin co ban",
    "ten xe",
    "nhien lieu",
    "so cho",
]
DOCUMENT_QUERY_TAG_RULES = {
    "gia_xe": [
        "gia",
        "gia xe",
        "gia ban",
        "gia khoi diem",
        "bang gia",
        "niem yet",
    ],
    "thong_so": [
        "thong so",
        "dong co",
        "kich thuoc",
        "cong suat",
        "mo men",
        "hop so",
        "nhien lieu",
        "tieu thu",
        "tiet kiem xang",
        "tiet kiem nhien lieu",
        "xang",
        "l/100km",
        "lit/100km",
    ],
    "bao_duong": ["bao duong", "bao tri", "phu tung", "sua chua"],
    "lai_thu": ["lai thu", "dat lich lai thu", "dang ky lai thu"],
    "tra_gop": ["tra gop", "vay mua xe", "tai chinh", "ngan hang"],
    "an_toan": ["an toan", "tui khi", "phanh", "camera", "canh bao"],
    "khuyen_mai": ["khuyen mai", "uu dai"],
    "bao_hanh": ["bao hanh"],
    "thu_tuc": ["thu tuc", "ho so", "giay to", "dang ky", "dang ki", "quy trinh"],
    "so_sanh": ["so sanh", "khac nhau"],
    "tu_van_chon_xe": ["tu van", "chon xe", "phu hop", "nhu cau"],
}
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

# Validate Groq API key.
if not GROQ_API_KEY:
    print("GROQ_API_KEY is not set; Groq calls will fail.")

groq_client = Groq(api_key=GROQ_API_KEY) if GROQ_API_KEY else None

SYSTEM_PROMPT = """
Bạn là trợ lý tư vấn mua xe chuyên nghiệp của TOYOTA Việt Nam.

Nguyên tắc chung:
- Luôn trả lời bằng tiếng Việt, giọng văn chuyên nghiệp, thân thiện.
- Chỉ dùng thông tin có trong dữ liệu tham khảo và ngữ cảnh hội thoại được cung cấp.
- Không bịa giá, thông số, phiên bản, trang bị hoặc khuyến mãi.
- Không đề cập đến bất kì xe nào khác ngoài dữ liệu tham khảo.
- Nếu dữ liệu không đủ, nói rõ phần nào chưa có thông tin thay vì suy đoán.
- Không tự đặt câu hỏi follow-up ở cuối câu trả lời; hệ thống sẽ xử lý việc hỏi thêm.
Phạm vi dữ liệu tham khảo:
- Hệ thống đã tự lọc nguồn trước khi gửi dữ liệu cho bạn.
- Nếu người dùng hỏi thông tin chung, danh mục xe, hãng có những xe/dòng xe nào, quy trình mua xe, đặt lịch lái thử, bảo dưỡng hoặc kinh nghiệm lựa chọn xe mà không nhắc một mẫu xe/dòng xe cụ thể, dữ liệu tham khảo sẽ đến từ tài liệu tư vấn cơ bản.
- Chỉ khi người dùng hỏi về một mẫu xe cụ thể hoặc một dòng xe cụ thể như Vios, Wigo, Hilux, Sedan, SUV, MPV/đa dụng, bán tải hoặc Hatchback, dữ liệu tham khảo mới đến từ nhóm tài liệu xe. Hãy ưu tiên trả lời đúng xe/dòng xe được hỏi, không dùng kiến thức chung để bù vào giá, thông số, phiên bản hoặc trang bị chưa có trong nguồn.
- Nếu dữ liệu tham khảo không đúng hoặc không đủ cho câu hỏi, hãy nói rõ chưa có thông tin trong dữ liệu hiện tại.

Cách trả lời theo loại câu hỏi:
- Nếu người dùng hỏi thông tin chung về danh mục xe, mua xe, sử dụng xe, bảo dưỡng, tài chính, an toàn hoặc kinh nghiệm lựa chọn xe, hãy trả lời bằng thông tin chung có trong nguồn.
- Nếu người dùng hỏi thông tin về một mẫu xe cụ thể, hãy giới thiệu trực tiếp mẫu xe đó: tổng quan, phiên bản, giá, thông số, trang bị nếu có trong dữ liệu.
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



# ── LLM helpers ───────────────────────────────────────────────────────────────


def _ask_groq(messages: List[Dict]) -> str:
    """Gọi Groq Chat Completions để sinh câu trả lời từ messages."""
    if not groq_client:
        raise RuntimeError("Groq client chưa được khởi tạo (thiếu API key)")
    resp = groq_client.chat.completions.create(
        model=GROQ_MODEL,
        messages=messages,
        temperature=0.2,
        max_tokens=1024,
    )
    return resp.choices[0].message.content


def _generate(messages: List[Dict]) -> tuple[str, str]:
    """Sinh câu trả lời bằng provider LLM hiện được hỗ trợ và trả kèm tên model."""
    return _ask_groq(messages), GROQ_MODEL


def _build_context(retrieved: List[Dict[str, Any]]) -> str:
    """Ghép các chunk truy xuất thành block ngữ cảnh đưa vào prompt."""
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
    """Xác định query có cần thêm lịch sử gần đây để retrieve chính xác hơn không."""
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
    """Tạo câu truy vấn retrieval từ câu hỏi hiện tại, lịch sử và slot đã biết."""
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
    """Chuẩn hóa text về dạng không dấu, chữ thường để so khớp keyword."""
    normalized = unicodedata.normalize("NFD", text.lower().replace("đ", "d"))
    normalized = "".join(
        char for char in normalized if unicodedata.category(char) != "Mn"
    )
    return re.sub(r"\s+", " ", normalized).strip()


def _contains_lookup_keyword(normalized_text: str, keyword: str) -> bool:
    """Kiểm tra keyword xuất hiện như một token/phrase độc lập trong text đã normalize."""
    normalized_keyword = _normalize_lookup_text(keyword)
    pattern = rf"(?<![a-z0-9]){re.escape(normalized_keyword)}(?![a-z0-9])"
    return re.search(pattern, normalized_text) is not None


def _mentions_vehicle_detail(query: str, state: ConversationState) -> bool:
    """Nhận diện câu hỏi có nhắc tới mẫu xe hoặc dòng xe cụ thể không."""
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
    """Kiểm tra text đã normalize có chứa keyword xe Toyota hoặc nhóm xe không."""
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


def _unique_values(values: List[str]) -> List[str]:
    """Loại bỏ giá trị trùng lặp nhưng vẫn giữ nguyên thứ tự ưu tiên."""
    unique = []
    seen = set()
    for value in values:
        if value and value not in seen:
            seen.add(value)
            unique.append(value)
    return unique


def _legacy_sources_for_categories(categories: List[str]) -> List[str]:
    """Lấy source cũ theo category để hỗ trợ dữ liệu Qdrant chưa re-index."""
    sources = []
    for category in categories:
        sources.extend(LEGACY_DOCUMENT_SOURCES_BY_CATEGORY.get(category, []))
    return _unique_values(sources)


def _document_category_for_doc(doc: Dict[str, Any]) -> str | None:
    """Lấy category ổn định của chunk, fallback bằng source/content khi cần."""
    return doc.get("document_category") or infer_document_category(
        f"{doc.get('source') or ''}\n{doc.get('content') or ''}"
    )


def _preferred_document_categories_for_query(query: str, state: ConversationState) -> List[str]:
    """Suy ra nhóm tài liệu nên ưu tiên dựa trên keyword trong query/ngữ cảnh."""
    lookup_text = _normalize_lookup_text(query)
    previous_user_turns = [
        msg["content"]
        for msg in state.get_recent_history(n_turns=2)
        if msg.get("role") == "user"
    ]
    if previous_user_turns and _needs_history_for_retrieval(query):
        lookup_text = _normalize_lookup_text(
            f"{query}\n" + "\n".join(previous_user_turns)
        )

    category_rules = [
        (
            [
                "hilux",
                "ban tai",
                "pickup",
            ],
            DOCUMENT_CATEGORY_HILUX,
        ),
        (
            [
                "sedan",
                "vios",
                "camry",
                "corolla",
                "altis",
            ],
            DOCUMENT_CATEGORY_SEDAN,
        ),
        (
            [
                "suv",
                "fortuner",
                "land cruiser",
                "prado",
                "yaris cross",
                "corolla cross",
                "raize",
            ],
            DOCUMENT_CATEGORY_SUV,
        ),
        (
            [
                "wigo",
                "hatchback",
            ],
            DOCUMENT_CATEGORY_WIGO,
        ),
        (
            [
                "mpv",
                "da dung",
                "7 cho",
                "7 cho ngoi",
                "gia dinh",
                "avanza",
                "veloz",
                "innova",
                "alphard",
            ],
            DOCUMENT_CATEGORY_MPV,
        ),
    ]

    preferred = []
    for keywords, category in category_rules:
        if any(
            _contains_lookup_keyword(lookup_text, keyword)
            for keyword in keywords
            if not (
                keyword == "corolla"
                and _contains_lookup_keyword(lookup_text, "corolla cross")
            )
        ):
            preferred.append(category)
    return _unique_values(preferred)


def _mentioned_specific_models(query: str, state: ConversationState) -> List[str]:
    """Trích các model Toyota cụ thể được nhắc trong query hoặc lịch sử gần."""
    lookup_text = _normalize_lookup_text(query)
    if _needs_history_for_retrieval(query):
        previous_user_turns = [
            msg["content"]
            for msg in state.get_recent_history(n_turns=2)
            if msg.get("role") == "user"
        ]
        if previous_user_turns:
            lookup_text = _normalize_lookup_text(
                f"{query}\n" + "\n".join(previous_user_turns)
            )

    models = [
        keyword
        for keyword in SPECIFIC_MODEL_KEYWORDS
        if _contains_lookup_keyword(lookup_text, keyword)
    ]
    models.sort(key=len, reverse=True)
    return models


def _metadata_values(doc: Dict[str, Any], key: str) -> List[str]:
    """Read metadata values from Qdrant payloads that may store strings or lists."""
    value = doc.get(key)
    if isinstance(value, list):
        return [str(item) for item in value if item]
    if isinstance(value, str) and value:
        return [value]
    return []


def _preferred_document_tags_for_query(query: str, state: ConversationState) -> List[str]:
    """Infer document tags that should be preferred for this query."""
    lookup_text = _normalize_lookup_text(query)
    if _needs_history_for_retrieval(query):
        previous_user_turns = [
            msg["content"]
            for msg in state.get_recent_history(n_turns=2)
            if msg.get("role") == "user"
        ]
        if previous_user_turns:
            lookup_text = _normalize_lookup_text(
                f"{query}\n" + "\n".join(previous_user_turns)
            )

    tags = []
    for tag, keywords in DOCUMENT_QUERY_TAG_RULES.items():
        if any(_contains_lookup_keyword(lookup_text, keyword) for keyword in keywords):
            tags.append(tag)
    return _unique_values(tags)


def _keyword_terms_for_tags(tags: List[str]) -> List[str]:
    """Return normalized lexical terms that indicate the preferred tags."""
    terms = []
    for tag in tags:
        terms.extend(DOCUMENT_QUERY_TAG_RULES.get(tag, []))
    return _unique_values(_normalize_lookup_text(term) for term in terms)


def _is_overview_query(query: str) -> bool:
    """Nhận diện câu hỏi yêu cầu giới thiệu/tổng quan/thông tin chung về xe."""
    normalized_text = _normalize_lookup_text(query)
    return any(marker in normalized_text for marker in OVERVIEW_QUERY_MARKERS)


def _lexical_support_docs(
    query: str,
    state: ConversationState,
    document_categories: List[str],
    legacy_source_names: List[str],
    *,
    limit: int = 8,
) -> List[Dict[str, Any]]:
    """Tìm thêm chunk bằng lexical match khi query nhắc rõ model xe."""
    mentioned_models = _mentioned_specific_models(query, state)
    preferred_tags = _preferred_document_tags_for_query(query, state)
    preferred_terms = _keyword_terms_for_tags(preferred_tags)
    if not mentioned_models and not preferred_terms:
        return []

    all_docs = scroll_chunks(
        source_names=legacy_source_names,
        document_categories=document_categories,
        limit=1000,
    )
    scored_docs = []
    overview_query = _is_overview_query(query)

    for doc in all_docs:
        normalized_doc = _normalize_lookup_text(
            f"{doc.get('source') or ''}\n{doc.get('content') or ''}"
        )
        doc_tags = set(_metadata_values(doc, "document_tags"))
        doc_models = set(_metadata_values(doc, "mentioned_models"))
        matched_models = [
            model
            for model in mentioned_models
            if _contains_lookup_keyword(normalized_doc, model)
            or model in doc_models
        ]
        matching_tags = set(preferred_tags) & doc_tags
        matching_terms = [
            term
            for term in preferred_terms
            if term and term in normalized_doc
        ]
        if mentioned_models and not matched_models:
            continue
        if not mentioned_models and not matching_tags and not matching_terms:
            continue

        lexical_score = 0.5
        lexical_score += 0.1 * len(matched_models)
        lexical_score += min(0.25, 0.08 * len(matching_tags))
        lexical_score += min(0.25, 0.04 * len(matching_terms))
        if overview_query:
            for term in HIGH_VALUE_VEHICLE_TERMS:
                if term in normalized_doc:
                    lexical_score += 0.08
        if any(term in normalized_doc for term in ("gia", "gia khoi diem")):
            lexical_score += 0.15
        if any(term in normalized_doc for term in ("tieu thu", "nhien lieu", "l/100km", "lit/100km")):
            lexical_score += 0.18
        if any(term in normalized_doc for term in ("phien ban", "tong quan", "thong tin co ban")):
            lexical_score += 0.12

        adjusted = dict(doc)
        adjusted["score"] = max(float(doc.get("score") or 0), lexical_score)
        adjusted["lexical_match"] = True
        adjusted["matched_models"] = matched_models
        adjusted["matched_tags"] = sorted(matching_tags)
        scored_docs.append(adjusted)

    scored_docs.sort(key=lambda item: item.get("score", 0), reverse=True)
    return scored_docs[:limit]


def _metadata_support_docs(
    query: str,
    state: ConversationState,
    document_categories: List[str],
    legacy_source_names: List[str],
    *,
    limit: int = 50,
) -> List[Dict[str, Any]]:
    """Find extra chunks through auto-label metadata tags/models."""
    preferred_tags = _preferred_document_tags_for_query(query, state)
    mentioned_models = _mentioned_specific_models(query, state)
    if not preferred_tags and not mentioned_models:
        return []

    all_docs = scroll_chunks(
        document_categories=document_categories,
        document_tags=preferred_tags,
        mentioned_models=mentioned_models,
        limit=limit,
    )
    scored_docs = []
    allowed_categories = set(document_categories)
    allowed_sources = set(legacy_source_names)

    for doc in all_docs:
        document_category = _document_category_for_doc(doc)
        source = doc.get("source")
        if document_category not in allowed_categories and source not in allowed_sources:
            continue

        doc_tags = set(_metadata_values(doc, "document_tags"))
        doc_models = set(_metadata_values(doc, "mentioned_models"))
        matching_tags = set(preferred_tags) & doc_tags
        matching_models = set(mentioned_models) & doc_models
        if not matching_tags and not matching_models:
            continue

        adjusted = dict(doc)
        adjusted["score"] = max(
            float(doc.get("score") or 0),
            0.5
            + min(0.25, 0.08 * len(matching_tags))
            + min(0.25, 0.12 * len(matching_models)),
        )
        adjusted["metadata_match"] = True
        adjusted["matched_tags"] = sorted(matching_tags)
        adjusted["matched_models"] = sorted(matching_models)
        scored_docs.append(adjusted)

    scored_docs.sort(key=lambda item: item.get("score", 0), reverse=True)
    return scored_docs[:limit]


def _merge_retrieval_candidates(
    semantic_docs: List[Dict[str, Any]],
    lexical_docs: List[Dict[str, Any]],
) -> List[Dict[str, Any]]:
    """Gộp kết quả semantic và lexical theo khóa chunk, giữ bản có điểm cao hơn."""
    merged = []
    by_key = {}
    for doc in [*semantic_docs, *lexical_docs]:
        key = _retrieved_doc_key(doc)
        existing = by_key.get(key)
        if existing is None:
            by_key[key] = doc
            merged.append(doc)
            continue
        if float(doc.get("score") or 0) > float(existing.get("score") or 0):
            existing.update(doc)
    return merged


def _is_need_based_query(query: str, state: ConversationState) -> bool:
    """Nhận diện query tư vấn chọn xe theo nhu cầu hoặc slot đã thu thập."""
    normalized_text = _normalize_lookup_text(query)
    need_markers = [
        "tu van",
        "chon xe",
        "mua xe",
        "phu hop",
        "nhu cau",
        "gia dinh",
        "kinh doanh",
        "di lai",
        "duong dai",
        "thanh pho",
        "dia hinh",
        "offroad",
        "off road",
        "cho",
        "ngan sach",
        "bang gia",
        "gia khoi diem",
        "tiet kiem xang",
        "tiet kiem nhien lieu",
        "tieu thu",
        "nhien lieu",
        "xang",
    ]
    if any(marker in normalized_text for marker in need_markers):
        return True

    filled_slots = state.get_filled_slots()
    return any(
        filled_slots.get(key)
        for key in ("budget", "seats", "purpose", "fuel", "region", "type_car")
    )


def _is_general_catalog_query(query: str) -> bool:
    """Nhận diện câu hỏi danh mục/danh sách xe tổng quát."""
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


def _is_general_information_query(query: str) -> bool:
    """Nhận diện câu hỏi thông tin chung không gắn với mẫu/dòng xe cụ thể."""
    normalized_text = _normalize_lookup_text(query)
    if _contains_vehicle_keyword(normalized_text):
        return False

    if _is_general_catalog_query(query):
        return True

    return any(marker in normalized_text for marker in GENERAL_INFORMATION_QUERY_MARKERS)


def _document_scope_for_query(
    query: str,
    state: ConversationState,
) -> tuple[str, List[str]]:
    """Chọn scope tài liệu và danh sách category được phép retrieve cho query."""
    if _is_general_information_query(query):
        return "general", GENERAL_DOCUMENT_CATEGORIES

    preferred_categories = _preferred_document_categories_for_query(query, state)
    if _mentions_vehicle_detail(query, state):
        if preferred_categories:
            return "vehicle", preferred_categories

        return (
            "vehicle",
            VEHICLE_DOCUMENT_CATEGORIES,
        )

    if _is_offroad_need(query, state) or _is_need_based_query(query, state):
        return "hybrid", HYBRID_DOCUMENT_CATEGORIES

    return "general", GENERAL_DOCUMENT_CATEGORIES


def _is_offroad_need(query: str, state: ConversationState) -> bool:
    """Xác định người dùng có nhu cầu đi địa hình/off-road không."""
    normalized_query = _normalize_lookup_text(query)
    normalized_offroad_keywords = [
        _normalize_lookup_text(keyword) for keyword in OFFROAD_KEYWORDS
    ]
    if any(keyword in normalized_query for keyword in normalized_offroad_keywords):
        return True
    slots = state.get_filled_slots()
    normalized_region = _normalize_lookup_text(str(slots.get("region") or ""))
    normalized_purpose = _normalize_lookup_text(str(slots.get("purpose") or ""))
    return normalized_region == "dia hinh" or normalized_purpose == "off-road"


def _expand_retrieval_query_for_domain(query: str, offroad_need: bool) -> str:
    """Bổ sung keyword nghiệp vụ vào retrieval query cho nhu cầu off-road."""
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
    query: str,
    state: ConversationState,
    document_scope: str,
    offroad_need: bool,
    limit: int,
) -> List[Dict[str, Any]]:
    """Rerank chunk truy xuất theo category ưu tiên, model được nhắc và nhu cầu domain."""
    normalized_query = _normalize_lookup_text(query)
    preferred_categories = set(_preferred_document_categories_for_query(query, state))
    mentioned_models = _mentioned_specific_models(query, state)
    preferred_tags = set(_preferred_document_tags_for_query(query, state))
    preferred_terms = _keyword_terms_for_tags(list(preferred_tags))
    catalog_query = _is_general_catalog_query(query)
    need_based_query = _is_need_based_query(query, state)
    overview_query = _is_overview_query(query)
    ranked = []

    for doc in retrieved:
        source = doc.get("source") or ""
        document_category = _document_category_for_doc(doc)
        normalized_doc = _normalize_lookup_text(
            f"{source}\n{doc.get('content') or ''}"
        )
        text = f"{source}\n{doc.get('content') or ''}".lower()
        score = float(doc.get("score") or 0)
        doc_tags = set(_metadata_values(doc, "document_tags"))
        doc_models = set(_metadata_values(doc, "mentioned_models"))

        if document_category in preferred_categories:
            score += 0.35
        elif preferred_categories and document_category in VEHICLE_DOCUMENT_CATEGORIES:
            score -= 0.05

        matching_tags = preferred_tags & doc_tags
        if matching_tags:
            score += min(0.3, 0.12 * len(matching_tags))

        if mentioned_models:
            matched_specific_model = any(
                _contains_lookup_keyword(normalized_doc, model)
                or model in doc_models
                for model in mentioned_models
            )
            if matched_specific_model:
                score += 0.45
                if overview_query and any(
                    term in normalized_doc for term in HIGH_VALUE_VEHICLE_TERMS
                ):
                    score += 0.25
            elif document_category in VEHICLE_DOCUMENT_CATEGORIES:
                score -= 0.35

        if doc.get("lexical_match"):
            score += 0.25
        if doc.get("metadata_match"):
            score += 0.18

        matching_terms = [
            term
            for term in preferred_terms
            if term and term in normalized_doc
        ]
        if matching_terms:
            score += min(0.25, 0.04 * len(matching_terms))

        if catalog_query and document_category == DOCUMENT_CATEGORY_SUMMARY:
            score += 0.45
        if need_based_query and document_category == DOCUMENT_CATEGORY_BASIC_ADVICE:
            score += 0.08

        for keyword in TOYOTA_MODEL_KEYWORDS:
            if _contains_lookup_keyword(normalized_query, keyword) and (
                _contains_lookup_keyword(normalized_doc, keyword)
                or keyword in doc_models
            ):
                score += 0.12

        filled_slots = state.get_filled_slots()
        for value in filled_slots.values():
            if value is not None and str(value).strip():
                normalized_value = _normalize_lookup_text(str(value))
                if normalized_value and normalized_value in normalized_doc:
                    score += 0.08

        if offroad_need and any(model in text for model in OFFROAD_PREFERRED_MODELS):
            score += 0.35
        if offroad_need and any(term in text for term in OFFROAD_FEATURE_TERMS):
            score += 0.25
        if offroad_need and any(model in text for model in OFFROAD_DOWNRANK_MODELS):
            score -= 0.25
            if not any(term in text for term in OFFROAD_FEATURE_TERMS):
                score -= 0.25

        adjusted = dict(doc)
        adjusted["document_category"] = document_category
        adjusted["rerank_score"] = score
        adjusted["document_scope"] = document_scope
        ranked.append(adjusted)

    ranked.sort(key=lambda item: item.get("rerank_score", item.get("score", 0)), reverse=True)
    return ranked[:limit]


def _retrieved_doc_key(doc: Dict[str, Any]) -> tuple[Any, Any, Any, Any]:
    """Tạo khóa định danh một chunk retrieved để chống trùng lặp."""
    return (
        doc.get("chunk_id"),
        doc.get("source_id"),
        doc.get("page"),
        doc.get("chunk_index"),
    )


def _add_context_doc(
    context_docs: List[Dict[str, Any]],
    seen: set,
    doc: Dict[str, Any],
    max_chunks: int,
    max_chars: int,
) -> bool:
    """Thêm chunk vào context nếu chưa trùng và chưa vượt giới hạn dung lượng."""
    key = _retrieved_doc_key(doc)
    if key in seen:
        return True

    current_chars = sum(len(item.get("content") or "") for item in context_docs)
    if current_chars + len(doc.get("content") or "") > max_chars:
        return False

    seen.add(key)
    context_docs.append(doc)
    return len(context_docs) < max_chunks


def _expand_with_neighbor_context(
    retrieved: List[Dict[str, Any]],
    *,
    max_chunks: int = MAX_CONTEXT_CHUNKS,
    max_chars: int = MAX_CONTEXT_CHARS,
) -> List[Dict[str, Any]]:
    """Mở rộng context bằng chunk liền kề quanh các kết quả retrieve chính."""
    expanded = []
    seen = set()

    for doc in retrieved:
        if len(expanded) >= max_chunks:
            break

        neighbors = search_neighbor_chunks(
            source_id=doc.get("source_id"),
            page=doc.get("page"),
            chunk_index=doc.get("chunk_index"),
            window=1,
        )
        before = [
            item
            for item in neighbors
            if (item.get("chunk_index") or -1) < (doc.get("chunk_index") or -1)
        ]
        after = [
            item
            for item in neighbors
            if (item.get("chunk_index") or -1) > (doc.get("chunk_index") or -1)
        ]

        ordered_docs = [*before, doc, *after]
        for item in ordered_docs:
            if item is not doc:
                item = {
                    **item,
                    "score": doc.get("score", item.get("score", 0)),
                    "rerank_score": doc.get("rerank_score"),
                    "neighbor_of": doc.get("chunk_id"),
                }
            if not _add_context_doc(expanded, seen, item, max_chunks, max_chars):
                return expanded

    return expanded


def _domain_answer_instruction(offroad_need: bool) -> str:
    """Tạo chỉ dẫn bổ sung cho LLM khi câu hỏi liên quan off-road."""
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
    user_id: str | None = None,
) -> Dict[str, Any]:
    """Chạy toàn bộ pipeline intent, rule, slot, retrieval và generation cho một query."""

    # ── Lấy / tạo conversation state ─────────────────────────────────────────
    state: ConversationState = state_manager.get_or_create(session_id, user_id=user_id)

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
            user_id=user_id,
            question=query,
        )

    warning_prefix = f"{rule_response}\n\n---\n\n" if rule_response else ""

    # ── Bước 3: Slot extraction ───────────────────────────────────────────────
    use_slot_context = should_extract_slots(intent)
    if use_slot_context:
        state.update_slots(extract_slots(query))
    else:
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
    document_scope, document_categories = _document_scope_for_query(query, state)
    legacy_source_names = _legacy_sources_for_categories(document_categories)

    if not prelim_decision.skip_rag:
        # Tăng top_k khi hỏi liệt kê
        list_keywords = ["tat ca", "nhung xe", "co nhung", "danh sach", "cac xe", "neu"]
        normalized_query = _normalize_lookup_text(query)
        list_query = _is_general_catalog_query(query) or any(
            kw in normalized_query for kw in list_keywords
        )
        dynamic_top_k = LIST_CONTEXT_TOP_K if list_query else TOP_K
        retrieval_top_k = RETRIEVAL_CANDIDATES_TOP_K

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
            f"Document scope: {document_scope} | categories: {', '.join(document_categories)}"
        )
        query_vec = embed_query(retrieval_query)
        retrieved = search(
            query_vec,
            top_k=retrieval_top_k,
            score_threshold=0.35,
            source_names=legacy_source_names,
            document_categories=document_categories,
        )
        if not retrieved:
            retrieved = search(
                query_vec,
                top_k=retrieval_top_k,
                score_threshold=0.2,
                source_names=legacy_source_names,
                document_categories=document_categories,
            )
        lexical_docs = _lexical_support_docs(
            query,
            state,
            document_categories,
            legacy_source_names,
        )
        metadata_docs = _metadata_support_docs(
            query,
            state,
            document_categories,
            legacy_source_names,
        )
        retrieved = _merge_retrieval_candidates(retrieved, [*lexical_docs, *metadata_docs])
        retrieved = _rerank_retrieved_docs(
            retrieved,
            query=query,
            state=state,
            document_scope=document_scope,
            offroad_need=offroad_need,
            limit=dynamic_top_k,
        )
        retrieved = _expand_with_neighbor_context(retrieved)

        if retrieved:
            rag_context = _build_context(retrieved)
            sources = [
                {
                    "source": r.get("source"),
                    "document_category": r.get("document_category"),
                    "page": r["page"],
                    "score": round(r["score"], 3),
                    "chunk_id": r.get("chunk_id"),
                    "chunk_index": r.get("chunk_index"),
                    "document_id": r.get("document_id"),
                    "gridfs_file_id": r.get("gridfs_file_id"),
                    "document_tags": r.get("document_tags") or [],
                    "mentioned_models": r.get("mentioned_models") or [],
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
                user_id=user_id,
                question=query,
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
        user_id=user_id,
        question=query,
    )


def _make_result(
    answer: str,
    sources: list,
    model_used: str,
    intent: str,
    rule_name: str,
    state: ConversationState,
    session_id: str,
    user_id: str | None = None,
    question: str = "",
) -> Dict[str, Any]:
    """Lưu state/chat log và đóng gói response chuẩn cho API."""
    answer = _append_response_links(
        answer,
        query=question,
        intent=intent,
        sources=sources,
    )
    state_manager.save(state)
    state_manager.log_chat_message(
        session_id=session_id,
        user_id=user_id or state.user_id,
        question=question,
        answer=answer,
        intent=intent,
        stage=state.stage,
        slots_snapshot=state.get_filled_slots(),
        sources=sources,
        model_used=model_used,
        rule_triggered=rule_name,
    )
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
def _append_response_links(
    answer: str,
    *,
    query: str,
    intent: str,
    sources: list,
) -> str:
    """Append frontend links when possible, without letting link lookup break RAG."""
    try:
        return append_relevant_links(
            answer,
            query=query,
            intent=intent,
            sources=sources,
        )
    except Exception as exc:
        print(f"[WARN] Khong the gan link lien quan vao cau tra loi: {exc}")
        return answer


if __name__ == "__main__":
    import uuid

    SESSION_ID = str(uuid.uuid4())
    print(f" Toyota RAG Chatbot  |  session: {SESSION_ID}")
    print(f" LLM provider: {LLM_PROVIDER.upper()}  (model: {GROQ_MODEL})")
    print("Lệnh đặc biệt: 'reset' | 'state' | 'exit'\n")

    while True:
        query = input("Bạn: ").strip()
        if not query:
            continue
        if query.lower() in ("exit", "quit", "thoát"):
            break
        if query.lower() == "reset":
            state_manager.reset(SESSION_ID)
            print(" Đã reset hội thoại.\n")
            continue
        if query.lower() == "state":
            s = state_manager.get(SESSION_ID)
            print(f" {s.summary() if s else 'Không có state'}\n")
            continue

        result = answer(query, session_id=SESSION_ID)
        print(
            f"\n ({result['model_used']}) [intent={result['intent']} | stage={result['stage']}]:"
        )
        print(result["answer"])
        print(f"\n Slots : {result['slots']}")
        print(f" Nguồn : {result['sources']}")
        print(f" Rule  : {result['rule_triggered']}\n")
        print("-" * 60)
