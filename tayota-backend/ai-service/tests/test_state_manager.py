from conversation_state_manager import ConversationState, MongoConversationStateManager


class FakeReplaceResult:
    pass


class FakeDeleteResult:
    pass


class FakeInsertResult:
    pass


class FakeCursor:
    def __init__(self, items):
        self.items = list(items)

    def sort(self, key, direction):
        reverse = direction < 0
        self.items.sort(key=lambda item: item.get(key), reverse=reverse)
        return self

    def skip(self, count):
        self.items = self.items[count:]
        return self

    def limit(self, count):
        self.items = self.items[:count]
        return self

    def __iter__(self):
        return iter(self.items)


class FakeCollection:
    def __init__(self):
        self.data = {}
        self.inserted = []

    def replace_one(self, query, document, upsert=False):
        self.data[query["_id"]] = dict(document)
        return FakeReplaceResult()

    def find_one(self, query):
        key = query.get("_id") or query.get("job_id")
        value = self.data.get(key)
        return dict(value) if value else None

    def delete_one(self, query):
        self.data.pop(query["_id"], None)
        return FakeDeleteResult()

    def count_documents(self, query):
        status = query.get("status")
        return sum(1 for item in self.data.values() if item.get("status") == status)

    def insert_one(self, document):
        self.inserted.append(dict(document))
        return FakeInsertResult()

    def find(self, query):
        items = [*self.data.values(), *self.inserted]
        return FakeCursor(
            dict(item)
            for item in items
            if all(item.get(key) == value for key, value in query.items())
        )


class FakeMongoStateManager(MongoConversationStateManager):
    def __init__(self):
        super().__init__(mongo_uri="mongodb://test", db_name="test")
        self.fake_sessions = FakeCollection()
        self.fake_messages = FakeCollection()

    @property
    def sessions(self):
        return self.fake_sessions

    @property
    def messages(self):
        return self.fake_messages


def test_mongo_state_manager_round_trips_slots_and_history():
    manager = FakeMongoStateManager()
    state = ConversationState(session_id="session-1", user_id="u1")
    state.slots["budget"] = "1 ty"
    state.add_turn("Toi can xe 7 cho", "Toyota co mot so lua chon phu hop.")

    manager.save(state)
    loaded = manager.get("session-1")

    assert loaded is not None
    assert loaded.user_id == "u1"
    assert loaded.get_filled_slots() == {"budget": "1 ty"}
    assert loaded.history == state.history
    assert loaded.turn_count == 1


def test_mongo_state_manager_reset_deletes_old_state():
    manager = FakeMongoStateManager()
    state = ConversationState(session_id="session-2", user_id="u2")
    state.add_turn("Cu", "Cu")
    manager.save(state)

    reset_state = manager.reset("session-2", user_id="u2")

    assert reset_state.session_id == "session-2"
    assert reset_state.user_id == "u2"
    assert reset_state.history == []
    assert reset_state.turn_count == 0


def test_mongo_state_manager_logs_chat_message():
    manager = FakeMongoStateManager()

    manager.log_chat_message(
        session_id="session-3",
        user_id="u3",
        question="Xe nao 7 cho?",
        answer="Toyota co mot so lua chon 7 cho.",
        intent="car_advice",
        stage="advising",
        slots_snapshot={"seats": 7},
        sources=[{"source": "TOYOTA SUV.pdf", "page": 1}],
        model_used="test-model",
        rule_triggered="",
    )

    assert len(manager.fake_messages.inserted) == 1
    saved = manager.fake_messages.inserted[0]
    assert saved["session_id"] == "session-3"
    assert saved["user_id"] == "u3"
    assert saved["question"] == "Xe nao 7 cho?"
    assert saved["answer"] == "Toyota co mot so lua chon 7 cho."


def test_mongo_state_manager_lists_chat_messages_by_session():
    manager = FakeMongoStateManager()
    for session_id, question in [
        ("session-4", "Cau 1"),
        ("other-session", "Khong lay"),
        ("session-4", "Cau 2"),
    ]:
        manager.log_chat_message(
            session_id=session_id,
            user_id="u4",
            question=question,
            answer=f"Tra loi {question}",
            intent="car_advice",
            stage="advising",
            slots_snapshot={},
            sources=[],
            model_used="test-model",
            rule_triggered="",
        )

    messages = manager.list_chat_messages(session_id="session-4", limit=10)

    assert [message["question"] for message in messages] == ["Cau 1", "Cau 2"]
    assert "_id" not in messages[0]


def test_mongo_state_manager_lists_sessions_by_user_newest_first():
    manager = FakeMongoStateManager()
    older = ConversationState(session_id="session-old", user_id="u5")
    older.add_turn("Cu", "Cu")
    older.updated_at = 100
    newer = ConversationState(session_id="session-new", user_id="u5")
    newer.updated_at = 200
    newer.slots["seats"] = 7
    other = ConversationState(session_id="other", user_id="other-user")
    manager.save(older)
    manager.save(newer)
    manager.save(other)

    sessions = manager.list_user_sessions(user_id="u5", limit=10)

    assert [session["session_id"] for session in sessions] == [
        "session-new",
        "session-old",
    ]
    assert sessions[0]["filled_slots"] == {"seats": 7}
    assert sessions[1]["history_len"] == 2
