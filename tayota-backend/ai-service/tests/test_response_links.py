import response_links
from response_links import append_relevant_links


def test_appends_vehicle_detail_link(monkeypatch):
    monkeypatch.setattr(response_links, "FRONTEND_BASE_URL", "http://frontend")
    monkeypatch.setattr(
        response_links,
        "_find_catalog_vehicle",
        lambda name: {"id": "vehicle-1", "name": name},
    )

    answer = append_relevant_links(
        "Toyota Veloz Cross co 7 cho.",
        query="Cho toi thong tin chi tiet Veloz Cross",
        intent="car_info",
        sources=[],
    )

    assert "Xem chi tiết xe: http://frontend/vehicles/vehicle-1" in answer


def test_appends_catalog_search_link_when_vehicle_id_not_found(monkeypatch):
    monkeypatch.setattr(response_links, "FRONTEND_BASE_URL", "http://frontend")
    monkeypatch.setattr(response_links, "_find_catalog_vehicle", lambda name: None)

    answer = append_relevant_links(
        "Toyota Avanza Premio la xe MPV 7 cho.",
        query="Cho toi thong tin chi tiet Avanza Premio",
        intent="car_info",
        sources=[],
    )

    assert "Xem danh sách xe phù hợp: http://frontend/vehicles?versionKeyword=avanza%20premio" in answer


def test_appends_service_booking_link_without_catalog_lookup(monkeypatch):
    calls = []
    monkeypatch.setattr(response_links, "FRONTEND_BASE_URL", "http://frontend")
    monkeypatch.setattr(response_links, "_find_catalog_vehicle", lambda name: calls.append(name))

    answer = append_relevant_links(
        "Anh chi co the dat lich bao duong truc tuyen.",
        query="Tôi muốn đặt lịch bảo dưỡng",
        intent="car_advice",
        sources=[],
    )

    assert "Đặt lịch dịch vụ: http://frontend/appointments/service" in answer
    assert calls == []


def test_appends_test_drive_link_with_vehicle_id(monkeypatch):
    monkeypatch.setattr(response_links, "FRONTEND_BASE_URL", "http://frontend")
    monkeypatch.setattr(
        response_links,
        "_find_catalog_vehicle",
        lambda name: {"id": "vios-1", "name": name},
    )

    answer = append_relevant_links(
        "Ban co the dang ky lai thu Toyota Vios.",
        query="Dang ky lai thu Vios",
        intent="car_advice",
        sources=[],
    )

    assert "Đăng ký lái thử: http://frontend/appointments/test-drive?carVersionId=vios-1" in answer


def test_does_not_duplicate_existing_link(monkeypatch):
    monkeypatch.setattr(response_links, "FRONTEND_BASE_URL", "http://frontend")

    answer = append_relevant_links(
        "Dat lich dich vu: http://frontend/appointments/service",
        query="Dat lich dich vu",
        intent="car_advice",
        sources=[],
    )

    assert answer.count("http://frontend/appointments/service") == 1
