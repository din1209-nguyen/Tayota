from rag import _clean_reference_phrasing


def test_clean_reference_phrasing_removes_internal_rag_labels():
    answer = (
        "Theo nguồn 1, nguồn 2: Toyota Vios có nhiều phiên bản phù hợp đi phố. "
        "(Dựa trên dữ liệu tham khảo)"
    )

    cleaned = _clean_reference_phrasing(answer)

    assert cleaned == "Toyota Vios có nhiều phiên bản phù hợp đi phố."


def test_clean_reference_phrasing_keeps_natural_answer_unchanged():
    answer = "Toyota Vios phù hợp đi phố nhờ kích thước gọn và mức giá dễ tiếp cận."

    assert _clean_reference_phrasing(answer) == answer
