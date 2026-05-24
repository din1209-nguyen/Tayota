import os
import time
import uuid
from dataclasses import asdict, dataclass, field
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional

from dotenv import load_dotenv

from slot_extractor import empty_slots

load_dotenv()

try:
    from pymongo import MongoClient
    from pymongo.errors import PyMongoError
except ImportError:  # pragma: no cover - only hit before dependencies are installed.
    MongoClient = None

    class PyMongoError(Exception):
        pass


MAX_HISTORY_TURNS = 10
SESSION_TTL_SECS = int(os.getenv("SESSION_TTL_SECS", "3600"))

STAGE_GREETING = "greeting"
STAGE_COLLECTING = "collecting"
STAGE_ADVISING = "advising"
STAGE_DONE = "done"


class MongoStateError(RuntimeError):
    pass


@dataclass
class ConversationState:
    session_id: str = field(default_factory=lambda: str(uuid.uuid4()))
    user_id: Optional[str] = None
    slots: Dict[str, Any] = field(default_factory=empty_slots)
    history: List[Dict[str, str]] = field(default_factory=list)
    intent_history: List[str] = field(default_factory=list)
    stage: str = STAGE_GREETING
    last_intent: Optional[str] = None
    turn_count: int = 0
    created_at: float = field(default_factory=time.time)
    updated_at: float = field(default_factory=time.time)
    status: str = "active"

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
            "user_id": self.user_id,
            "stage": self.stage,
            "turn_count": self.turn_count,
            "last_intent": self.last_intent,
            "filled_slots": self.get_filled_slots(),
            "missing_slots": self.get_missing_slots(),
            "history_len": len(self.history),
            "status": self.status,
        }


class ConversationStateManager:
    """In-memory state manager, kept for explicit local tests only."""

    def __init__(self):
        self._sessions: Dict[str, ConversationState] = {}
        self.chat_messages: List[Dict[str, Any]] = []

    def create(
        self,
        session_id: Optional[str] = None,
        user_id: Optional[str] = None,
    ) -> ConversationState:
        sid = session_id or str(uuid.uuid4())
        state = ConversationState(session_id=sid, user_id=user_id)
        self._sessions[sid] = state
        return state

    def get(self, session_id: str) -> Optional[ConversationState]:
        state = self._sessions.get(session_id)
        if state and state.is_expired():
            self.delete(session_id)
            return None
        return state

    def get_or_create(
        self,
        session_id: str,
        user_id: Optional[str] = None,
    ) -> ConversationState:
        state = self.get(session_id)
        if state is None:
            state = self.create(session_id, user_id=user_id)
        elif user_id and state.user_id != user_id:
            state.user_id = user_id
            state.updated_at = time.time()
            self.save(state)
        return state

    def save(self, state: ConversationState) -> None:
        self._sessions[state.session_id] = state

    def delete(self, session_id: str) -> None:
        self._sessions.pop(session_id, None)

    def reset(
        self,
        session_id: str,
        user_id: Optional[str] = None,
    ) -> ConversationState:
        self.delete(session_id)
        return self.create(session_id, user_id=user_id)

    def purge_expired(self) -> int:
        expired = [sid for sid, state in self._sessions.items() if state.is_expired()]
        for sid in expired:
            del self._sessions[sid]
        return len(expired)

    def active_count(self) -> int:
        return len(self._sessions)

    def all_summaries(self) -> List[Dict[str, Any]]:
        return [state.summary() for state in self._sessions.values()]

    def list_user_sessions(
        self,
        *,
        user_id: str,
        limit: int = 50,
        offset: int = 0,
    ) -> List[Dict[str, Any]]:
        sessions = [
            {
                **state.summary(),
                "created_at": datetime.fromtimestamp(
                    state.created_at,
                    tz=timezone.utc,
                ),
                "updated_at": datetime.fromtimestamp(
                    state.updated_at,
                    tz=timezone.utc,
                ),
            }
            for state in self._sessions.values()
            if state.user_id == user_id
        ]
        sessions.sort(key=lambda item: item.get("updated_at") or datetime.min, reverse=True)
        return sessions[offset : offset + limit]

    def log_chat_message(
        self,
        *,
        session_id: str,
        user_id: Optional[str],
        question: str,
        answer: str,
        intent: str,
        stage: str,
        slots_snapshot: Dict[str, Any],
        sources: List[Dict[str, Any]],
        model_used: str,
        rule_triggered: str,
    ) -> None:
        self.chat_messages.append(
            {
                "session_id": session_id,
                "user_id": user_id,
                "question": question,
                "answer": answer,
                "intent": intent,
                "stage": stage,
                "slots_snapshot": slots_snapshot,
                "sources": sources,
                "model_used": model_used,
                "rule_triggered": rule_triggered,
                "created_at": datetime.now(timezone.utc),
            }
        )

    def list_chat_messages(
        self,
        *,
        session_id: str,
        limit: int = 50,
        offset: int = 0,
    ) -> List[Dict[str, Any]]:
        messages = [
            dict(message)
            for message in self.chat_messages
            if message.get("session_id") == session_id
        ]
        messages.sort(key=lambda message: message.get("created_at") or datetime.min)
        return messages[offset : offset + limit]


class MongoConversationStateManager:
    """MongoDB-backed session and chat log store for the FastAPI service."""

    def __init__(
        self,
        mongo_uri: str | None = None,
        db_name: str | None = None,
        *,
        sessions_collection: str = "ai_sessions",
        messages_collection: str = "ai_chat_messages",
    ):
        self.mongo_uri = mongo_uri or os.getenv("MONGO_URI", "mongodb://localhost:27017")
        self.db_name = db_name or os.getenv("MONGO_DB", "tayota_ai_db")
        self.sessions_collection = sessions_collection
        self.messages_collection = messages_collection
        self._client = None
        self._db = None

    def _get_client(self):
        if MongoClient is None:
            raise MongoStateError(
                "MongoDB dependency is not installed. Run `pip install -r requirements.txt`."
            )
        if self._client is None:
            self._client = MongoClient(
                self.mongo_uri,
                serverSelectionTimeoutMS=2000,
                connectTimeoutMS=2000,
            )
        return self._client

    def _get_db(self):
        if self._db is None:
            self._db = self._get_client()[self.db_name]
        return self._db

    @property
    def sessions(self):
        return self._get_db()[self.sessions_collection]

    @property
    def messages(self):
        return self._get_db()[self.messages_collection]

    def ping(self) -> bool:
        try:
            self._get_client().admin.command("ping")
            return True
        except PyMongoError as exc:
            raise MongoStateError(
                f"Cannot connect to MongoDB at {self.mongo_uri}: {exc}"
            ) from exc

    def create(
        self,
        session_id: Optional[str] = None,
        user_id: Optional[str] = None,
    ) -> ConversationState:
        sid = session_id or str(uuid.uuid4())
        state = ConversationState(session_id=sid, user_id=user_id)
        self.save(state)
        return state

    def get(self, session_id: str) -> Optional[ConversationState]:
        try:
            payload = self.sessions.find_one({"_id": session_id})
        except PyMongoError as exc:
            raise MongoStateError(
                f"Cannot load session '{session_id}' from MongoDB: {exc}"
            ) from exc

        if not payload:
            return None

        return self._state_from_document(payload)

    def get_or_create(
        self,
        session_id: str,
        user_id: Optional[str] = None,
    ) -> ConversationState:
        state = self.get(session_id)
        if state is None:
            return self.create(session_id, user_id=user_id)
        if user_id and state.user_id != user_id:
            state.user_id = user_id
            state.updated_at = time.time()
            self.save(state)
        return state

    def save(self, state: ConversationState) -> None:
        document = self._state_to_document(state)
        try:
            self.sessions.replace_one(
                {"_id": state.session_id},
                document,
                upsert=True,
            )
        except PyMongoError as exc:
            raise MongoStateError(
                f"Cannot save session '{state.session_id}' to MongoDB: {exc}"
            ) from exc

    def delete(self, session_id: str) -> None:
        try:
            self.sessions.delete_one({"_id": session_id})
        except PyMongoError as exc:
            raise MongoStateError(
                f"Cannot delete session '{session_id}' from MongoDB: {exc}"
            ) from exc

    def reset(
        self,
        session_id: str,
        user_id: Optional[str] = None,
    ) -> ConversationState:
        self.delete(session_id)
        return self.create(session_id, user_id=user_id)

    def active_count(self) -> int:
        try:
            return self.sessions.count_documents({"status": "active"})
        except PyMongoError as exc:
            raise MongoStateError(f"Cannot count MongoDB sessions: {exc}") from exc

    def list_user_sessions(
        self,
        *,
        user_id: str,
        limit: int = 50,
        offset: int = 0,
    ) -> List[Dict[str, Any]]:
        try:
            cursor = (
                self.sessions.find({"user_id": user_id})
                .sort("updated_at", -1)
                .skip(offset)
                .limit(limit)
            )
            return [self._session_summary_from_document(session) for session in cursor]
        except PyMongoError as exc:
            raise MongoStateError(
                f"Cannot load sessions for user '{user_id}' from MongoDB: {exc}"
            ) from exc

    def log_chat_message(
        self,
        *,
        session_id: str,
        user_id: Optional[str],
        question: str,
        answer: str,
        intent: str,
        stage: str,
        slots_snapshot: Dict[str, Any],
        sources: List[Dict[str, Any]],
        model_used: str,
        rule_triggered: str,
    ) -> None:
        try:
            self.messages.insert_one(
                {
                    "session_id": session_id,
                    "user_id": user_id,
                    "question": question,
                    "answer": answer,
                    "intent": intent,
                    "stage": stage,
                    "slots_snapshot": slots_snapshot,
                    "sources": sources,
                    "model_used": model_used,
                    "rule_triggered": rule_triggered,
                    "created_at": datetime.now(timezone.utc),
                }
            )
        except PyMongoError as exc:
            raise MongoStateError(
                f"Cannot save chat log for session '{session_id}' to MongoDB: {exc}"
            ) from exc

    def list_chat_messages(
        self,
        *,
        session_id: str,
        limit: int = 50,
        offset: int = 0,
    ) -> List[Dict[str, Any]]:
        try:
            cursor = (
                self.messages.find({"session_id": session_id})
                .sort("created_at", 1)
                .skip(offset)
                .limit(limit)
            )
            messages = []
            for message in cursor:
                payload = dict(message)
                payload.pop("_id", None)
                messages.append(payload)
            return messages
        except PyMongoError as exc:
            raise MongoStateError(
                f"Cannot load chat messages for session '{session_id}' from MongoDB: {exc}"
            ) from exc

    def _state_to_document(self, state: ConversationState) -> Dict[str, Any]:
        payload = asdict(state)
        payload["_id"] = state.session_id
        payload["recent_history"] = payload.pop("history")
        payload["created_at_iso"] = datetime.fromtimestamp(
            state.created_at,
            tz=timezone.utc,
        )
        payload["updated_at_iso"] = datetime.fromtimestamp(
            state.updated_at,
            tz=timezone.utc,
        )
        return payload

    def _state_from_document(self, document: Dict[str, Any]) -> ConversationState:
        payload = dict(document)
        payload.pop("_id", None)
        payload.pop("created_at_iso", None)
        payload.pop("updated_at_iso", None)
        if "recent_history" in payload:
            payload["history"] = payload.pop("recent_history")
        allowed = ConversationState.__dataclass_fields__.keys()
        return ConversationState(**{k: v for k, v in payload.items() if k in allowed})

    def _session_summary_from_document(self, document: Dict[str, Any]) -> Dict[str, Any]:
        slots = document.get("slots") or {}
        history = document.get("recent_history") or []
        return {
            "session_id": document.get("session_id") or document.get("_id"),
            "user_id": document.get("user_id"),
            "stage": document.get("stage"),
            "turn_count": document.get("turn_count", 0),
            "last_intent": document.get("last_intent"),
            "filled_slots": {key: value for key, value in slots.items() if value is not None},
            "history_len": len(history),
            "status": document.get("status", "active"),
            "created_at": document.get("created_at_iso") or document.get("created_at"),
            "updated_at": document.get("updated_at_iso") or document.get("updated_at"),
        }


def _build_state_manager():
    backend = os.getenv("STATE_BACKEND", "mongo").lower()
    if backend == "memory":
        return ConversationStateManager()
    if backend != "mongo":
        raise ValueError("STATE_BACKEND must be 'mongo' or 'memory'.")
    return MongoConversationStateManager()


state_manager = _build_state_manager()

# Backward-compatible names for existing imports/tests during transition.
RedisStateError = MongoStateError
