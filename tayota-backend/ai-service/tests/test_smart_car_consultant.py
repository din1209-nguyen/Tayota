from conversation_state_manager import ConversationState
from logic_smart_car_consultant import smart_consultant


def test_consultant_does_not_follow_up_for_missing_slots():
    state = ConversationState(session_id="session-1")
    state.update_stage("car_advice")
    state.slots["budget"] = 1000

    decision = smart_consultant.decide(
        "Toi can xe Toyota cho gia dinh",
        state,
        rag_context="Toyota co cac dong xe phu hop cho gia dinh.",
    )

    assert decision.followup is None
    assert smart_consultant.compose_final_response("Cau tra loi", decision) == "Cau tra loi"
