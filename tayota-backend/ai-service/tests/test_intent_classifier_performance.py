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


def test_rule_first_intent_accepts_test_drive_service_query(monkeypatch):
    monkeypatch.setattr(intent_classifier, "groq_client", ExplodingGroq())

    result = intent_classifier.classify_intent("hướng dẫn đặt lịch lái thử")

    assert result["intent"] == "car_advice"
    assert result["confidence"] >= intent_classifier.INTENT_RULE_CONFIDENCE_THRESHOLD


def test_rule_first_intent_accepts_maintenance_booking_query(monkeypatch):
    monkeypatch.setattr(intent_classifier, "groq_client", ExplodingGroq())

    result = intent_classifier.classify_intent("hướng dẫn đặt lịch bảo dưỡng")

    assert result["intent"] == "car_advice"
    assert result["confidence"] >= intent_classifier.INTENT_RULE_CONFIDENCE_THRESHOLD


def test_rule_first_intent_accepts_vehicle_breakdown_service_query(monkeypatch):
    monkeypatch.setattr(intent_classifier, "groq_client", ExplodingGroq())

    result = intent_classifier.classify_intent("xe bị hỏng thì nên làm gì")

    assert result["intent"] == "car_advice"
    assert result["confidence"] >= intent_classifier.INTENT_RULE_CONFIDENCE_THRESHOLD


def test_rule_first_intent_still_blocks_unrelated_query(monkeypatch):
    monkeypatch.setattr(intent_classifier, "groq_client", ExplodingGroq())

    result = intent_classifier.classify_intent("hom nay nen nau mon gi")

    assert result["intent"] == "out_of_scope"
    assert result["confidence"] >= intent_classifier.INTENT_RULE_CONFIDENCE_THRESHOLD


def test_rule_first_intent_accepts_fuel_saving_preference(monkeypatch):
    monkeypatch.setattr(intent_classifier, "groq_client", ExplodingGroq())

    result = intent_classifier.classify_intent("uu tien tiet kiem xang")

    assert result["intent"] == "car_advice"
    assert result["confidence"] >= intent_classifier.INTENT_RULE_CONFIDENCE_THRESHOLD


def test_missing_groq_key_uses_local_fallback(monkeypatch):
    monkeypatch.setattr(intent_classifier, "groq_client", None)

    result = intent_classifier.classify_intent("Noi gi do kha mo ho")

    assert result["intent"] in intent_classifier.INTENTS
    assert "confidence" in result
