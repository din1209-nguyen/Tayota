import json
import os
import re
import unicodedata
from dataclasses import dataclass
from typing import Any, Dict, Iterable, List

from dotenv import load_dotenv
from groq import Groq


load_dotenv()

DOCUMENT_CATEGORIES = {
    "summary",
    "basic_advice",
    "hilux",
    "sedan",
    "suv",
    "wigo",
    "mpv",
}

DOCUMENT_TAGS = {
    "gia_xe",
    "thong_so",
    "bao_duong",
    "lai_thu",
    "tra_gop",
    "an_toan",
    "khuyen_mai",
    "bao_hanh",
    "thu_tuc",
    "so_sanh",
    "tu_van_chon_xe",
}

TOYOTA_MODELS = [
    "alphard",
    "altis",
    "avanza",
    "avanza premio",
    "camry",
    "corolla",
    "corolla altis",
    "corolla cross",
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

TAG_RULES = {
    "gia_xe": ["gia", "gia xe", "gia ban", "niem yet", "du kien gia"],
    "thong_so": [
        "thong so",
        "dong co",
        "kich thuoc",
        "cong suat",
        "mo men",
        "hop so",
        "nhien lieu",
        "l/100km",
        "mm",
        "kw",
        "hp",
        "nm",
    ],
    "bao_duong": ["bao duong", "bao tri", "lich bao duong", "phu tung"],
    "lai_thu": ["lai thu", "dang ky lai thu", "dat lich lai thu"],
    "tra_gop": ["tra gop", "vay mua xe", "tai chinh", "ngan hang"],
    "an_toan": ["an toan", "tui khi", "abs", "vsc", "phanh", "canh bao", "camera"],
    "khuyen_mai": ["khuyen mai", "uu dai", "qua tang"],
    "bao_hanh": ["bao hanh", "thoi han bao hanh"],
    "thu_tuc": ["thu tuc", "ho so", "dang ky", "dang ki", "giay to"],
    "so_sanh": ["so sanh", "khac nhau", "hon", "kem"],
    "tu_van_chon_xe": ["tu van", "chon xe", "phu hop", "nhu cau", "gia dinh"],
}


@dataclass
class DocumentLabels:
    document_category: str | None
    document_tags: List[str]
    mentioned_models: List[str]
    label_source: str
    label_confidence: float

    def as_metadata(self) -> Dict[str, Any]:
        return {
            "document_category": self.document_category,
            "document_tags": self.document_tags,
            "mentioned_models": self.mentioned_models,
            "label_source": self.label_source,
            "label_confidence": self.label_confidence,
        }


def normalize_text(text: str | None) -> str:
    text = (text or "").casefold().replace("đ", "d")
    normalized = unicodedata.normalize("NFD", text)
    normalized = "".join(
        char for char in normalized if unicodedata.category(char) != "Mn"
    )
    return re.sub(r"\s+", " ", normalized).strip()


def _contains(text: str, keyword: str) -> bool:
    normalized_keyword = normalize_text(keyword)
    return re.search(
        rf"(?<![a-z0-9]){re.escape(normalized_keyword)}(?![a-z0-9])",
        text,
    ) is not None


def _unique(values: Iterable[str]) -> List[str]:
    result = []
    seen = set()
    for value in values:
        if value and value not in seen:
            seen.add(value)
            result.append(value)
    return result


def _rule_category(text: str) -> tuple[str | None, float]:
    if "summary" in text:
        return "summary", 0.95
    if any(marker in text for marker in ("tu van co ban", "basic advice")):
        return "basic_advice", 0.9
    if any(marker in text for marker in ("hilux", "ban tai", "pickup")):
        return "hilux", 0.9
    if any(marker in text for marker in ("sedan", "vios", "camry", "altis")):
        return "sedan", 0.86
    if any(
        marker in text
        for marker in (
            "suv",
            "fortuner",
            "land cruiser",
            "prado",
            "yaris cross",
            "corolla cross",
            "raize",
        )
    ):
        return "suv", 0.86
    if any(marker in text for marker in ("wigo", "hatchback")):
        return "wigo", 0.9
    if any(
        marker in text
        for marker in ("mpv", "da dung", "7 cho", "avanza", "veloz", "innova", "alphard")
    ):
        return "mpv", 0.86
    return None, 0.45


def label_document_by_rules(
    *,
    source: str | None,
    text: str,
    existing_category: str | None = None,
) -> DocumentLabels:
    lookup = normalize_text(f"{source or ''}\n{text}")
    document_category, confidence = _rule_category(lookup)
    if existing_category in DOCUMENT_CATEGORIES:
        document_category = existing_category
        confidence = max(confidence, 0.92)

    tags = [
        tag
        for tag, keywords in TAG_RULES.items()
        if any(_contains(lookup, keyword) for keyword in keywords)
    ]
    mentioned_models = [
        model for model in TOYOTA_MODELS if _contains(lookup, model)
    ]

    if document_category and (tags or mentioned_models):
        confidence = max(confidence, 0.78)
    elif tags or mentioned_models:
        confidence = max(confidence, 0.62)

    return DocumentLabels(
        document_category=document_category,
        document_tags=_unique(tags),
        mentioned_models=_unique(sorted(mentioned_models, key=len, reverse=True)),
        label_source="rule",
        label_confidence=round(min(confidence, 0.99), 2),
    )


def _parse_llm_json(content: str) -> Dict[str, Any]:
    match = re.search(r"\{.*\}", content, flags=re.DOTALL)
    if not match:
        raise ValueError("LLM response does not contain JSON")
    return json.loads(match.group(0))


def _sanitize_llm_labels(raw: Dict[str, Any], fallback: DocumentLabels) -> DocumentLabels:
    category = raw.get("document_category")
    if category not in DOCUMENT_CATEGORIES:
        category = fallback.document_category

    tags = [
        tag for tag in raw.get("document_tags", []) if tag in DOCUMENT_TAGS
    ]
    models = [
        normalize_text(model)
        for model in raw.get("mentioned_models", [])
        if normalize_text(model) in TOYOTA_MODELS
    ]
    confidence = raw.get("label_confidence", fallback.label_confidence)
    try:
        confidence = float(confidence)
    except (TypeError, ValueError):
        confidence = fallback.label_confidence

    return DocumentLabels(
        document_category=category,
        document_tags=_unique([*fallback.document_tags, *tags]),
        mentioned_models=_unique([*fallback.mentioned_models, *models]),
        label_source="hybrid",
        label_confidence=round(max(fallback.label_confidence, min(confidence, 0.99)), 2),
    )


def label_document_with_llm(
    *,
    source: str | None,
    text: str,
    fallback: DocumentLabels,
) -> DocumentLabels:
    api_key = os.getenv("GROQ_API_KEY", "")
    if not api_key:
        return fallback

    client = Groq(api_key=api_key)
    model = os.getenv("GROQ_MODEL", "llama-3.3-70b-versatile")
    prompt = {
        "source": source,
        "allowed_document_categories": sorted(DOCUMENT_CATEGORIES),
        "allowed_document_tags": sorted(DOCUMENT_TAGS),
        "allowed_models": TOYOTA_MODELS,
        "text_sample": text[:6000],
    }
    response = client.chat.completions.create(
        model=model,
        messages=[
            {
                "role": "system",
                "content": (
                    "You label Toyota Vietnam RAG documents. Return JSON only with "
                    "document_category, document_tags, mentioned_models, label_confidence. "
                    "Use only allowed values."
                ),
            },
            {"role": "user", "content": json.dumps(prompt, ensure_ascii=False)},
        ],
        temperature=0,
        max_tokens=300,
    )
    raw = _parse_llm_json(response.choices[0].message.content or "")
    return _sanitize_llm_labels(raw, fallback)


def label_document(
    *,
    source: str | None,
    text: str,
    existing_category: str | None = None,
    llm_confidence_threshold: float = 0.75,
) -> DocumentLabels:
    rule_labels = label_document_by_rules(
        source=source,
        text=text,
        existing_category=existing_category,
    )
    if rule_labels.label_confidence >= llm_confidence_threshold:
        return rule_labels

    try:
        return label_document_with_llm(
            source=source,
            text=text,
            fallback=rule_labels,
        )
    except Exception as exc:
        print(f"[WARN] Auto-label LLM fallback failed for {source}: {exc}")
        return rule_labels


def apply_document_labels(documents: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """Attach one stable label set per source to every extracted page."""
    grouped: Dict[str, List[Dict[str, Any]]] = {}
    for doc in documents:
        metadata = doc.get("metadata", {})
        key = (
            metadata.get("source_id")
            or metadata.get("source_path")
            or metadata.get("source")
            or "unknown"
        )
        grouped.setdefault(str(key), []).append(doc)

    for pages in grouped.values():
        first_metadata = pages[0].get("metadata", {})
        source = first_metadata.get("source") or first_metadata.get("source_path")
        existing_category = first_metadata.get("document_category")
        sample = "\n\n".join(page.get("content", "") for page in pages[:6])
        labels = label_document(
            source=source,
            text=sample,
            existing_category=existing_category,
        )
        for page in pages:
            metadata = page.setdefault("metadata", {})
            metadata.update(labels.as_metadata())

    return documents
