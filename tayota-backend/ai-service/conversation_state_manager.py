import json
import os
import time
import uuid
from dataclasses import asdict, dataclass, field
from typing import Any, Dict, List, Optional

from dotenv import load_dotenv

from slot_extractor import empty_slots

load_dotenv()

try:
    import redis
    from redis.exceptions import RedisError
except ImportError:  # pragma: no cover - only hit before dependencies are installed.
    redis = None

    class RedisError(Exception):
        pass


MAX_HISTORY_TURNS = 10
SESSION_TTL_SECS = int(os.getenv("SESSION_TTL_SECS", "3600"))

STAGE_GREETING = "greeting"
STAGE_COLLECTING = "collecting"
STAGE_ADVISING = "advising"
STAGE_DONE = "done"


@dataclass
class ConversationState:
    session_id: str = field(default_factory=lambda: str(uuid.uuid4()))
    slots: Dict[str, Any] = field(default_factory=empty_slots)
    history: List[Dict[str, str]] = field(default_factory=list)
    intent_history: List[str] = field(default_factory=list)
    stage: str = STAGE_GREETING
    last_intent: Optional[str] = None
    turn_count: int = 0
    created_at: float = field(default_factory=time.time)
    updated_at: float = field(default_factory=time.time)

    def update_slots(self, new_slots: Dict[str, Any]) -> None:
        from slot_extractor import merge_slots

        self.slots = merge_slots(self.slots, new_slots)
        self.updated_at = time.time()

    def get_filled_slots(self) -> Dict[str, Any]:
        return {k: v for k, v in self.slots.items() if v is not None}

    def get_missing_slots(self) -> List[str]:
        return [k for k, v in self.slots.items() if v is None]

    def has_enough_info(self) -> bool:
        key_slots = ["budget", "seats", "purpose"]
        return any(self.slots.get(slot) is not None for slot in key_slots)

    def add_turn(self, user_msg: str, assistant_msg: str) -> None:
        self.history.append({"role": "user", "content": user_msg})
        self.history.append({"role": "assistant", "content": assistant_msg})
        self.turn_count += 1
        self.updated_at = time.time()

        max_msgs = MAX_HISTORY_TURNS * 2
        if len(self.history) > max_msgs:
            self.history = self.history[-max_msgs:]

    def get_recent_history(self, n_turns: int = 3) -> List[Dict[str, str]]:
        return self.history[-(n_turns * 2) :]

    def update_stage(self, intent: str) -> None:
        self.last_intent = intent
        self.intent_history.append(intent)
        self.updated_at = time.time()

        if intent == "greeting" and self.turn_count == 0:
            self.stage = STAGE_GREETING
        elif intent in ("car_advice", "budget_filter", "seat_filter", "usage_filter"):
            self.stage = STAGE_ADVISING if self.has_enough_info() else STAGE_COLLECTING
        elif intent == "car_info":
            self.stage = STAGE_ADVISING

    def is_expired(self) -> bool:
        return (time.time() - self.updated_at) > SESSION_TTL_SECS

    def summary(self) -> Dict[str, Any]:
        return {
            "session_id": self.session_id,
            "stage": self.stage,
            "turn_count": self.turn_count,
            "last_intent": self.last_intent,
            "filled_slots": self.get_filled_slots(),
            "missing_slots": self.get_missing_slots(),
            "history_len": len(self.history),
        }


class ConversationStateManager:
    """In-memory state manager, kept for explicit local tests only."""

    def __init__(self):
        self._sessions: Dict[str, ConversationState] = {}

    def create(self, session_id: Optional[str] = None) -> ConversationState:
        sid = session_id or str(uuid.uuid4())
        state = ConversationState(session_id=sid)
        self._sessions[sid] = state
        return state

    def get(self, session_id: str) -> Optional[ConversationState]:
        state = self._sessions.get(session_id)
        if state and state.is_expired():
            self.delete(session_id)
            return None
        return state

    def get_or_create(self, session_id: str) -> ConversationState:
        state = self.get(session_id)
        if state is None:
            state = self.create(session_id)
        return state

    def save(self, state: ConversationState) -> None:
        self._sessions[state.session_id] = state

    def delete(self, session_id: str) -> None:
        self._sessions.pop(session_id, None)

    def reset(self, session_id: str) -> ConversationState:
        self.delete(session_id)
        return self.create(session_id)

    def purge_expired(self) -> int:
        expired = [sid for sid, state in self._sessions.items() if state.is_expired()]
        for sid in expired:
            del self._sessions[sid]
        return len(expired)

    def active_count(self) -> int:
        return len(self._sessions)

    def all_summaries(self) -> List[Dict[str, Any]]:
        return [state.summary() for state in self._sessions.values()]


class RedisStateError(RuntimeError):
    pass


class RedisConversationStateManager:
    """Redis-backed session store for the FastAPI service."""

    def __init__(
        self,
        redis_url: str | None = None,
        *,
        ttl_seconds: int = SESSION_TTL_SECS,
        key_prefix: str = "ai-service:sessions",
    ):
        self.redis_url = redis_url or os.getenv("REDIS_URL", "redis://localhost:6379/0")
        self.ttl_seconds = ttl_seconds
        self.key_prefix = key_prefix.rstrip(":")
        self._client = None

    def _key(self, session_id: str) -> str:
        return f"{self.key_prefix}:{session_id}"

    def _get_client(self):
        if redis is None:
            raise RedisStateError(
                "Redis dependency is not installed. Run `pip install -r requirements.txt`."
            )
        if self._client is None:
            self._client = redis.Redis.from_url(
                self.redis_url,
                decode_responses=True,
                socket_connect_timeout=2,
                socket_timeout=2,
            )
        return self._client

    def ping(self) -> bool:
        try:
            return bool(self._get_client().ping())
        except RedisError as exc:
            raise RedisStateError(
                f"Cannot connect to Redis at {self.redis_url}: {exc}"
            ) from exc

    def create(self, session_id: Optional[str] = None) -> ConversationState:
        sid = session_id or str(uuid.uuid4())
        state = ConversationState(session_id=sid)
        self.save(state)
        return state

    def get(self, session_id: str) -> Optional[ConversationState]:
        try:
            payload = self._get_client().get(self._key(session_id))
        except RedisError as exc:
            raise RedisStateError(
                f"Cannot load session '{session_id}' from Redis: {exc}"
            ) from exc

        if not payload:
            return None

        try:
            state = ConversationState(**json.loads(payload))
        except (TypeError, ValueError) as exc:
            raise RedisStateError(
                f"Stored session '{session_id}' is invalid JSON/state data."
            ) from exc

        if state.is_expired():
            self.delete(session_id)
            return None
        return state

    def get_or_create(self, session_id: str) -> ConversationState:
        state = self.get(session_id)
        if state is None:
            state = self.create(session_id)
        return state

    def save(self, state: ConversationState) -> None:
        try:
            self._get_client().setex(
                self._key(state.session_id),
                self.ttl_seconds,
                json.dumps(asdict(state), ensure_ascii=False),
            )
        except RedisError as exc:
            raise RedisStateError(
                f"Cannot save session '{state.session_id}' to Redis: {exc}"
            ) from exc

    def delete(self, session_id: str) -> None:
        try:
            self._get_client().delete(self._key(session_id))
        except RedisError as exc:
            raise RedisStateError(
                f"Cannot delete session '{session_id}' from Redis: {exc}"
            ) from exc

    def reset(self, session_id: str) -> ConversationState:
        self.delete(session_id)
        return self.create(session_id)

    def active_count(self) -> int:
        try:
            return len(list(self._get_client().scan_iter(f"{self.key_prefix}:*")))
        except RedisError as exc:
            raise RedisStateError(f"Cannot scan Redis sessions: {exc}") from exc


def _build_state_manager():
    backend = os.getenv("STATE_BACKEND", "redis").lower()
    if backend == "memory":
        return ConversationStateManager()
    if backend != "redis":
        raise ValueError("STATE_BACKEND must be 'redis' or 'memory'.")
    return RedisConversationStateManager()


state_manager = _build_state_manager()
