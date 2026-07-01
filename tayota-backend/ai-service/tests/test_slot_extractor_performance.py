import slot_extractor


class ExplodingGroq:
    class chat:
        class completions:
            @staticmethod
            def create(**kwargs):
                raise AssertionError("Groq should not be called")


class FakeGroq:
    called = False

    class chat:
        class completions:
            @staticmethod
            def create(**kwargs):
                FakeGroq.called = True

                class Message:
                    content = '{"budget": 1000, "seats": null, "purpose": null, "fuel": null, "region": null, "type_car": null, "overrides": ["budget"], "clears": []}'

                class Choice:
                    message = Message()

                class Response:
                    choices = [Choice()]

                return Response()


def test_regex_first_slot_extraction_skips_groq_for_clear_slots(monkeypatch):
    monkeypatch.setattr(slot_extractor, "groq_client", ExplodingGroq())
    monkeypatch.setattr(slot_extractor, "ENABLE_LLM_SLOT_EXTRACTION", True)
    monkeypatch.setattr(slot_extractor, "SLOT_REGEX_FIRST", True)

    slots = slot_extractor.extract_slots("Toi can xe 7 cho xang di thanh pho")

    assert slots["seats"] == 7
    assert slots["fuel"] is not None
    assert slots["region"] is not None


def test_complex_slot_update_can_use_groq(monkeypatch):
    FakeGroq.called = False
    monkeypatch.setattr(slot_extractor, "groq_client", FakeGroq())
    monkeypatch.setattr(slot_extractor, "ENABLE_LLM_SLOT_EXTRACTION", True)
    monkeypatch.setattr(slot_extractor, "SLOT_REGEX_FIRST", True)

    slots = slot_extractor.extract_slots("Doi thanh 1 ty")

    assert FakeGroq.called is True
    assert slots["budget"] == 1000
    assert slots["overrides"] == ["budget"]


def test_merge_slots_implicitly_overrides_multiple_existing_slots(monkeypatch):
    monkeypatch.setattr(slot_extractor, "IMPLICIT_SLOT_OVERRIDE_ENABLED", True)
    existing = {
        **slot_extractor.empty_slots(),
        "seats": 7,
        "fuel": "hybrid",
        "budget": 1000,
    }
    new = {
        **slot_extractor.empty_slots(),
        "seats": 5,
        "fuel": "xang",
        "overrides": [],
        "clears": [],
    }

    merged = slot_extractor.merge_slots(existing, new)

    assert merged["seats"] == 5
    assert merged["fuel"] == "xang"
    assert merged["budget"] == 1000


def test_merge_slots_fills_empty_slots_when_multiple_values_present(monkeypatch):
    monkeypatch.setattr(slot_extractor, "IMPLICIT_SLOT_OVERRIDE_ENABLED", True)
    existing = slot_extractor.empty_slots()
    new = {
        **slot_extractor.empty_slots(),
        "seats": 7,
        "fuel": "hybrid",
        "overrides": [],
        "clears": [],
    }

    merged = slot_extractor.merge_slots(existing, new)

    assert merged["seats"] == 7
    assert merged["fuel"] == "hybrid"
