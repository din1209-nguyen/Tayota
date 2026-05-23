import json
from dataclasses import asdict
from typing import Any

from redis import Redis

from conversation_state_manager import ConversationState, SESSION_TTL_SECS


class RedisConversationStateManager:
    """Conversation state manager backed by Redis.

    Redis errors intentionally bubble up so API handlers can return a clear
    service error instead of silently switching to process memory.
    """

    def __init__(self, redis_client: Redis, ttl_seconds: int = SESSION_TTL_SECS):
        self.redis = redis_client
        self.ttl_seconds = ttl_seconds

    def _key(self, session_id: str) -> str:
        return f"conversation:{session_id}"

    def _serialize(self, state: ConversationState) -> str:
        return json.dumps(asdict(state), ensure_ascii=False)

    def _deserialize(self, raw: str | bytes) -> ConversationState:
        if isinstance(raw, bytes):
            raw = raw.decode("utf-8")
        payload: dict[str, Any] = json.loads(raw)
        return ConversationState(**payload)

    def _save(self, state: ConversationState) -> ConversationState:
        self.redis.setex(self._key(state.session_id), self.ttl_seconds, self._serialize(state))
        return state

    def create(self, session_id: str | None = None) -> ConversationState:
        state = ConversationState(session_id=session_id) if session_id else ConversationState()
        return self._save(state)

    def get(self, session_id: str) -> ConversationState | None:
        raw = self.redis.get(self._key(session_id))
        if raw is None:
            return None
        state = self._deserialize(raw)
        if state.is_expired():
            self.delete(session_id)
            return None
        self.redis.expire(self._key(session_id), self.ttl_seconds)
        return state

    def get_or_create(self, session_id: str) -> ConversationState:
        return self.get(session_id) or self.create(session_id)

    def save(self, state: ConversationState) -> ConversationState:
        return self._save(state)

    def delete(self, session_id: str) -> None:
        self.redis.delete(self._key(session_id))

    def reset(self, session_id: str) -> ConversationState:
        self.delete(session_id)
        return self.create(session_id)

    def purge_expired(self) -> int:
        return 0

    def active_count(self) -> int:
        return len(list(self.redis.scan_iter(match="conversation:*")))

    def all_summaries(self) -> list[dict[str, Any]]:
        summaries = []
        for key in self.redis.scan_iter(match="conversation:*"):
            raw = self.redis.get(key)
            if raw:
                summaries.append(self._deserialize(raw).summary())
        return summaries
