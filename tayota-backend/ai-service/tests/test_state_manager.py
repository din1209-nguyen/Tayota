from conversation_state_manager import ConversationState, RedisConversationStateManager


class FakeRedis:
    def __init__(self):
        self.data = {}

    def ping(self):
        return True

    def setex(self, key, ttl, value):
        self.data[key] = value

    def get(self, key):
        return self.data.get(key)

    def delete(self, key):
        self.data.pop(key, None)

    def scan_iter(self, pattern):
        prefix = pattern.rstrip("*")
        return [key for key in self.data if key.startswith(prefix)]


class FakeRedisStateManager(RedisConversationStateManager):
    def __init__(self):
        super().__init__(redis_url="redis://test", key_prefix="test:sessions")
        self.fake = FakeRedis()

    def _get_client(self):
        return self.fake


def test_redis_state_manager_round_trips_slots_and_history():
    manager = FakeRedisStateManager()
    state = ConversationState(session_id="session-1")
    state.slots["budget"] = "1 ty"
    state.add_turn("Toi can xe 7 cho", "Toyota co mot so lua chon phu hop.")

    manager.save(state)
    loaded = manager.get("session-1")

    assert loaded is not None
    assert loaded.get_filled_slots() == {"budget": "1 ty"}
    assert loaded.history == state.history
    assert loaded.turn_count == 1


def test_redis_state_manager_reset_deletes_old_state():
    manager = FakeRedisStateManager()
    state = ConversationState(session_id="session-2")
    state.add_turn("Cu", "Cu")
    manager.save(state)

    reset_state = manager.reset("session-2")

    assert reset_state.session_id == "session-2"
    assert reset_state.history == []
    assert reset_state.turn_count == 0
