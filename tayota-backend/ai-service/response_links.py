import os
import re
import unicodedata
from typing import Any
from urllib.parse import quote

import httpx


FRONTEND_BASE_URL = os.getenv("FRONTEND_BASE_URL", "http://localhost:3000").rstrip("/")
CATALOG_API_BASE_URL = os.getenv(
    "CATALOG_API_BASE_URL",
    "http://localhost:9090/car",
).rstrip("/")
CATALOG_TIMEOUT_SECS = float(os.getenv("CATALOG_TIMEOUT_SECS", "2.0"))

CATALOG_SEARCH_PATH = "/catalog/car-versions"

MODEL_NAMES = [
    "land cruiser prado",
    "land cruiser",
    "corolla cross",
    "corolla altis",
    "yaris cross",
    "avanza premio",
    "innova cross",
    "veloz cross",
    "alphard",
    "fortuner",
    "hilux",
    "camry",
    "vios",
    "wigo",
    "raize",
]

SERVICE_LINK_RULES = [
    (
        "/appointments/test-drive",
        [
            "lai thu",
            "test drive",
            "dang ky lai",
            "dat lich lai",
        ],
        "Đăng ký lái thử",
    ),
    (
        "/appointments/service",
        [
            "dat lich",
            "bao duong",
            "bao tri",
            "sua chua",
            "dich vu",
            "hen bao duong",
            "lich hen",
        ],
        "Đặt lịch dịch vụ",
    ),
    (
        "/dealerships",
        [
            "dai ly",
            "showroom",
            "lien he",
            "dia chi",
            "chi nhanh",
        ],
        "Danh sách đại lý",
    ),
]


def append_relevant_links(
    answer: str,
    *,
    query: str,
    intent: str,
    sources: list[dict[str, Any]] | None = None,
) -> str:
    """Append frontend links that are relevant to the answer, without changing API shape."""
    links: list[tuple[str, str]] = []
    vehicle_name = _single_vehicle_name(query, answer, sources or [])
    vehicle = _find_catalog_vehicle(vehicle_name) if vehicle_name else None
    service_link = _service_link_for_query(query, intent)

    if service_link:
        path, label = service_link
        if path == "/appointments/test-drive" and vehicle:
            links.append((label, f"{FRONTEND_BASE_URL}{path}?carVersionId={vehicle['id']}"))
        else:
            links.append((label, f"{FRONTEND_BASE_URL}{path}"))
    elif vehicle:
        links.append(("Xem chi tiết xe", f"{FRONTEND_BASE_URL}/vehicles/{vehicle['id']}"))
    elif vehicle_name:
        links.append(
            (
                "Xem danh sách xe phù hợp",
                f"{FRONTEND_BASE_URL}/vehicles?versionKeyword={quote(vehicle_name)}",
            )
        )

    return _append_links(answer, links)


def _append_links(answer: str, links: list[tuple[str, str]]) -> str:
    unique_links = []
    seen = set()
    for label, url in links:
        if not url or url in answer or url in seen:
            continue
        seen.add(url)
        unique_links.append((label, url))
    if not unique_links:
        return answer

    lines = [answer.rstrip(), ""]
    lines.extend(f"{label}: {url}" for label, url in unique_links)
    return "\n".join(lines)


def _service_link_for_query(query: str, intent: str) -> tuple[str, str] | None:
    text = _normalize(f"{query} {intent}")
    for path, keywords, label in SERVICE_LINK_RULES:
        if any(keyword in text for keyword in keywords):
            return path, label
    return None


def _single_vehicle_name(
    query: str,
    answer: str,
    sources: list[dict[str, Any]],
) -> str | None:
    query_matches = _vehicle_names_in_text(query)
    if len(query_matches) == 1:
        return query_matches[0]
    if len(query_matches) > 1:
        return None

    source_models = []
    for source in sources:
        for model in source.get("mentioned_models") or []:
            source_models.extend(_vehicle_names_in_text(str(model)))
    source_models = _unique(source_models)
    if len(source_models) == 1:
        return source_models[0]
    if len(source_models) > 1:
        return None

    answer_matches = _vehicle_names_in_text(answer)
    if len(answer_matches) == 1:
        return answer_matches[0]
    return None


def _vehicle_names_in_text(text: str) -> list[str]:
    normalized_text = _normalize(text)
    matches = [
        model
        for model in MODEL_NAMES
        if re.search(rf"(?<![a-z0-9]){re.escape(model)}(?![a-z0-9])", normalized_text)
    ]
    matches.sort(key=len, reverse=True)
    return _unique(matches)


def _find_catalog_vehicle(vehicle_name: str | None) -> dict[str, Any] | None:
    if not vehicle_name:
        return None
    try:
        response = httpx.get(
            f"{CATALOG_API_BASE_URL}{CATALOG_SEARCH_PATH}",
            params={"keyword": vehicle_name, "page": 0, "size": 10},
            timeout=CATALOG_TIMEOUT_SECS,
        )
        response.raise_for_status()
    except Exception as exc:
        print(f"[WARN] Khong lay duoc link catalog cho {vehicle_name}: {exc}")
        return None

    items = _catalog_items(response.json())
    return _best_vehicle_match(vehicle_name, items)


def _catalog_items(data: Any) -> list[dict[str, Any]]:
    if isinstance(data, dict) and "result" in data:
        data = data.get("result")
    if isinstance(data, dict):
        items = data.get("items") or data.get("content") or data.get("data") or []
    elif isinstance(data, list):
        items = data
    else:
        items = []
    return [item for item in items if isinstance(item, dict)]


def _best_vehicle_match(vehicle_name: str, items: list[dict[str, Any]]) -> dict[str, Any] | None:
    if not items:
        return None

    normalized_vehicle = _normalize(vehicle_name)
    scored_items = []
    for index, item in enumerate(items):
        item_name = _normalize(str(item.get("name") or ""))
        series_name = _normalize(str(item.get("carSeriesName") or ""))
        score = 0
        if item_name == normalized_vehicle:
            score += 4
        if normalized_vehicle in item_name:
            score += 3
        if normalized_vehicle in series_name:
            score += 2
        if item.get("id"):
            score += 1
        scored_items.append((score, -index, item))

    scored_items.sort(reverse=True, key=lambda entry: (entry[0], entry[1]))
    best = scored_items[0][2]
    return best if best.get("id") else None


def _unique(values: list[str]) -> list[str]:
    result = []
    seen = set()
    for value in values:
        if value and value not in seen:
            seen.add(value)
            result.append(value)
    return result


def _normalize(text: str) -> str:
    text = str(text or "").lower().replace("đ", "d")
    normalized = unicodedata.normalize("NFD", text)
    normalized = "".join(
        char for char in normalized if unicodedata.category(char) != "Mn"
    )
    return re.sub(r"\s+", " ", normalized).strip()
