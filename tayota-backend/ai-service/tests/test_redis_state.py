from ai_service.redis_state import RedisConversationStateManager


class FakeRedis:
    def __init__(self):
        self.store = {}

    def setex(self, key, ttl, value):
        self.store[key] = value

    def get(self, key):
        return self.store.get(key)

    def delete(self, key):
        self.store.pop(key, None)

    def expire(self, key, ttl):
        return key in self.store

    def scan_iter(self, match=None):
        prefix = (match or "").rstrip("*")
        return [key for key in self.store if key.startswith(prefix)]


def test_redis_state_save_and_load_preserves_slots_and_history():
    manager = RedisConversationStateManager(FakeRedis(), ttl_seconds=3600)
    state = manager.get_or_create("session-1")
    state.update_slots({"budget": "1 ty", "seats": 7})
    state.add_turn("Toi can xe 7 cho", "Toyota co mot so lua chon phu hop.")
    manager.save(state)

    loaded = manager.get("session-1")

    assert loaded is not None
    assert loaded.slots["budget"] == "1 ty"
    assert loaded.slots["seats"] == 7
    assert loaded.history[-1]["role"] == "assistant"
    assert loaded.turn_count == 1


def test_redis_state_reset_deletes_previous_state():
    manager = RedisConversationStateManager(FakeRedis(), ttl_seconds=3600)
    state = manager.get_or_create("session-1")
    state.add_turn("Cu", "Cu")
    manager.save(state)

    reset_state = manager.reset("session-1")

    assert reset_state.session_id == "session-1"
    assert reset_state.history == []
    assert reset_state.turn_count == 0
