import intent_classifier


class ExplodingGroq:
    class chat:
        class completions:
            @staticmethod
            def create(**kwargs):
                raise AssertionError("Groq should not be called")


def test_rule_first_intent_skips_groq_for_clear_price_query(monkeypatch):
    monkeypatch.setattr(intent_classifier, "groq_client", ExplodingGroq())

    result = intent_classifier.classify_intent("Gia Vios bao nhieu?")

    assert result["intent"] == "budget_filter"
    assert result["confidence"] >= intent_classifier.INTENT_RULE_CONFIDENCE_THRESHOLD


def test_rule_first_intent_skips_groq_for_greeting(monkeypatch):
    monkeypatch.setattr(intent_classifier, "groq_client", ExplodingGroq())

    result = intent_classifier.classify_intent("Xin chao")

    assert result["intent"] == "greeting"


def test_missing_groq_key_uses_local_fallback(monkeypatch):
    monkeypatch.setattr(intent_classifier, "groq_client", None)

    result = intent_classifier.classify_intent("Noi gi do kha mo ho")

    assert result["intent"] in intent_classifier.INTENTS
    assert "confidence" in result
