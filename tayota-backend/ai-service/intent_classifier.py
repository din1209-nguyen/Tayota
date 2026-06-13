import json
import os
import re
import unicodedata
from typing import Dict

from dotenv import load_dotenv
from groq import Groq

from performance import env_flag, env_float


load_dotenv()

GROQ_API_KEY = os.getenv("GROQ_API_KEY", "")
GROQ_MODEL = os.getenv("GROQ_MODEL", "llama-3.3-70b-versatile")
ENABLE_RULE_FIRST_INTENT = env_flag("ENABLE_RULE_FIRST_INTENT", "true")
INTENT_RULE_CONFIDENCE_THRESHOLD = env_float(
    "INTENT_RULE_CONFIDENCE_THRESHOLD",
    0.78,
)
groq_client = Groq(api_key=GROQ_API_KEY) if GROQ_API_KEY else None

INTENTS = {
    "greeting": "Chao hoi, cam on, tam biet, hoi bot la ai",
    "car_advice": "Hoi tu van mua xe, so sanh xe, hoi gia, thong so ky thuat Toyota",
    "car_info": "Hoi thong tin chung ve mot dong xe Toyota cu the",
    "budget_filter": "Hoi xe theo ngan sach / tam gia",
    "seat_filter": "Hoi xe theo so cho ngoi",
    "usage_filter": "Hoi xe theo muc dich dung",
    "out_of_scope": "Cau hoi khong lien quan den Toyota hoac mua xe",
    "sensitive": "Noi dung nhay cam, chinh tri, bao luc, 18+",
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

VEHICLE_CATEGORY_TERMS = [
    "sedan",
    "suv",
    "mpv",
    "hatchback",
    "ban tai",
    "pickup",
    "da dung",
]

CLASSIFIER_PROMPT = """
Ban la bo phan loai y dinh (intent classifier) cho chatbot tu van mua xe Toyota.

Danh sach intent va mo ta:
{intent_list}

Nhiem vu:
- Doc cau hoi cua nguoi dung
- Chon DUNG MOT intent phu hop nhat tu danh sach tren
- Tra ve JSON theo dinh dang sau, khong giai thich them:

{{
  "intent": "<ten intent>",
  "confidence": <so thuc 0.0 - 1.0>,
  "reason": "<giai thich ngan gon 1 cau>"
}}
""".strip()


def _normalize_text(text: str) -> str:
    text = (text or "").casefold().replace("đ", "d")
    text = unicodedata.normalize("NFD", text)
    text = "".join(char for char in text if unicodedata.category(char) != "Mn")
    return re.sub(r"\s+", " ", text).strip()


def _contains(text: str, keyword: str) -> bool:
    keyword = _normalize_text(keyword)
    return re.search(rf"(?<![a-z0-9]){re.escape(keyword)}(?![a-z0-9])", text) is not None


def _result(intent: str, confidence: float, reason: str) -> Dict:
    return {
        "intent": intent if intent in INTENTS else "car_advice",
        "confidence": round(max(0.0, min(confidence, 1.0)), 2),
        "reason": reason,
    }


def _mentions_toyota_or_vehicle(text: str) -> bool:
    if _contains(text, "toyota"):
        return True
    return any(_contains(text, term) for term in [*TOYOTA_MODELS, *VEHICLE_CATEGORY_TERMS, "xe", "oto", "o to"])


def classify_intent_by_rules(query: str) -> Dict:
    text = _normalize_text(query)
    if not text:
        return _result("car_advice", 0.5, "empty query fallback")

    if any(
        marker in text
        for marker in (
            "tu sat",
            "tu hai",
            "khieu dam",
            "18+",
            "bao luc",
            "chinh tri",
            "vu khi",
            "ma tuy",
        )
    ):
        return _result("sensitive", 0.92, "sensitive keyword rule")

    if any(
        _contains(text, marker)
        for marker in (
            "xin chao",
            "chao",
            "hello",
            "hi",
            "cam on",
            "thank",
            "tam biet",
            "bye",
            "ban la ai",
        )
    ) and len(text.split()) <= 8:
        return _result("greeting", 0.94, "greeting keyword rule")

    if any(
        _contains(text, marker)
        for marker in (
            "ngan sach",
            "tam gia",
            "gia",
            "gia bao nhieu",
            "bao nhieu tien",
            "duoi",
            "tren",
            "khoang",
            "tra gop",
            "vay mua xe",
        )
    ) or re.search(r"\d+(?:[.,]\d+)?\s*(?:ty|ti|trieu|tr\b)", text):
        return _result("budget_filter", 0.9, "budget/price keyword rule")

    if re.search(r"\d+\s*(?:cho|ghe|seat)", text) or any(
        marker in text for marker in ("bao nhieu cho", "may cho", "so cho")
    ):
        return _result("seat_filter", 0.9, "seat keyword rule")

    if any(
        marker in text
        for marker in (
            "gia dinh",
            "kinh doanh",
            "chay dich vu",
            "off-road",
            "offroad",
            "dia hinh",
            "duong dai",
            "thanh pho",
            "di pho",
            "nhu cau",
        )
    ):
        return _result("usage_filter", 0.86, "usage keyword rule")

    if any(marker in text for marker in ("tu van", "chon xe", "phu hop", "so sanh", "mua xe")):
        return _result("car_advice", 0.84, "advice keyword rule")

    if any(_contains(text, term) for term in [*TOYOTA_MODELS, *VEHICLE_CATEGORY_TERMS]):
        return _result("car_info", 0.84, "specific Toyota model/category rule")

    if not _mentions_toyota_or_vehicle(text):
        return _result("out_of_scope", 0.82, "no Toyota or vehicle signal")

    return _result("car_advice", 0.62, "low-confidence Toyota-related fallback")


def _classify_intent_with_groq(query: str) -> Dict:
    if not groq_client:
        return _result("car_advice", 0.5, "fallback because GROQ_API_KEY is missing")

    intent_list = "\n".join(f"- {key}: {value}" for key, value in INTENTS.items())
    messages = [
        {
            "role": "system",
            "content": CLASSIFIER_PROMPT.format(intent_list=intent_list),
        },
        {"role": "user", "content": f'Cau hoi: "{query}"'},
    ]

    response = groq_client.chat.completions.create(
        model=GROQ_MODEL,
        messages=messages,
        temperature=0.0,
        max_tokens=150,
    )
    raw = response.choices[0].message.content.strip()
    if raw.startswith("```"):
        raw = raw.split("```")[1]
        if raw.startswith("json"):
            raw = raw[4:]

    result = json.loads(raw)
    if result.get("intent") not in INTENTS:
        result["intent"] = "car_advice"
    result.setdefault("confidence", 0.5)
    result.setdefault("reason", "groq classifier")
    return result


def classify_intent(query: str) -> Dict:
    """
    Classify intent. Rule-first avoids a Groq call for clear queries, while
    keeping Groq fallback for ambiguous cases.
    """
    rule_result = classify_intent_by_rules(query)
    if (
        ENABLE_RULE_FIRST_INTENT
        and float(rule_result.get("confidence") or 0) >= INTENT_RULE_CONFIDENCE_THRESHOLD
    ):
        return rule_result

    try:
        return _classify_intent_with_groq(query)
    except Exception as exc:
        print(f"[WARN] Intent classifier failed ({exc}); fallback to local rule")
        return rule_result if rule_result.get("intent") else _result(
            "car_advice",
            0.5,
            "fallback after classifier error",
        )
