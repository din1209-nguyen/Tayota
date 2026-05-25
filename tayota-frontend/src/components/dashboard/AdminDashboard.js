"use client";

import { useEffect, useMemo, useState } from "react";
import { apiFetch } from "@/lib/api";
import { getAccessories, getAllCarVersions, getCarStylesWithVersions } from "@/lib/services/car";
import { unwrapList } from "@/lib/format";
import { EMPTY_VEHICLE_FILTERS, filterVehicleItems } from "@/lib/vehicle-filters";
import VehicleFilterControls from "@/components/vehicles/VehicleFilterControls";

export default function AdminDashboard() {
  const [vehicles, setVehicles] = useState([]);
  const [vehicleStyles, setVehicleStyles] = useState([]);
  const [vehicleFilters, setVehicleFilters] = useState(EMPTY_VEHICLE_FILTERS);
  const [accessories, setAccessories] = useState([]);
  const [accountForm, setAccountForm] = useState({
    email: "",
    password: "",
    role: "SERVICE_ADVISOR",
    dealershipId: "",
  });
  const [message, setMessage] = useState("");

  useEffect(() => {
    let active = true;

    Promise.all([getAllCarVersions(), getAccessories({ page: 0, size: 8 }), getCarStylesWithVersions()])
      .then(([nextVehicles, nextAccessories, nextStyles]) => {
        if (!active) return;
        setVehicles(nextVehicles);
        setAccessories(unwrapList(nextAccessories));
        setVehicleStyles(unwrapList(nextStyles));
      })
      .catch((error) => {
        if (active) setMessage(error.message);
      });

    return () => {
      active = false;
    };
  }, []);

  const filteredVehicles = useMemo(() => filterVehicleItems(vehicles, vehicleFilters), [vehicleFilters, vehicles]);

  function updateField(event) {
    setAccountForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  async function createAccount(event) {
    event.preventDefault();
    setMessage("");
    try {
      await apiFetch("/user/create-account", {
        method: "POST",
        body: JSON.stringify(accountForm),
      });
      setMessage("Tạo tài khoản thành công.");
    } catch (error) {
      setMessage(error.message);
    }
  }

  return (
    <div className="ops-grid">
      <nav className="role-tabs wide" aria-label="Admin sections">
        <a href="#admin-accounts">Tài khoản</a>
        <a href="#admin-catalog">Xe</a>
        <a href="#admin-accessories">Phụ kiện</a>
      </nav>

      <section className="ops-panel" id="admin-accounts">
        <p className="eyebrow">Admin</p>
        <h2>Tạo tài khoản nội bộ</h2>
        <form className="ops-form" onSubmit={createAccount}>
          <input className="field" name="email" type="email" placeholder="Email" value={accountForm.email} onChange={updateField} />
          <input className="field" name="password" type="password" placeholder="Mật khẩu" value={accountForm.password} onChange={updateField} />
          <select className="field" name="role" value={accountForm.role} onChange={updateField}>
            <option value="SERVICE_ADVISOR">SERVICE_ADVISOR</option>
            <option value="MECHANIC">MECHANIC</option>
            <option value="ASSISTANT">ASSISTANT</option>
            <option value="MANAGER">MANAGER</option>
            <option value="ADMIN">ADMIN</option>
          </select>
          <input className="field" name="dealershipId" placeholder="UUID đại lý nếu cần" value={accountForm.dealershipId} onChange={updateField} />
          <button className="btn btn-primary" type="submit">Tạo tài khoản</button>
        </form>
        {message ? <div className="status-box">{message}</div> : null}
      </section>

      <section className="ops-panel" id="admin-catalog">
        <p className="eyebrow">Catalog</p>
        <h2>Phiên bản xe</h2>
        <VehicleFilterControls
          filters={vehicleFilters}
          onChange={setVehicleFilters}
          onReset={() => setVehicleFilters(EMPTY_VEHICLE_FILTERS)}
          styles={vehicleStyles}
          variant="compact"
          advanced={false}
        />
        <div className="ops-list">
          {filteredVehicles.map((vehicle) => (
            <article key={vehicle.id || vehicle.carVersionId}>
              <strong>{vehicle.versionName || vehicle.name || vehicle.carVersionName}</strong>
              <span>{vehicle.carSeriesName || "Tayota"} - {vehicle.modelYear || "Đang cập nhật"}</span>
            </article>
          ))}
          {!filteredVehicles.length ? <div className="status-box">Không có phiên bản xe phù hợp bộ lọc.</div> : null}
        </div>
      </section>

      <section className="ops-panel" id="admin-accessories">
        <p className="eyebrow">Accessories</p>
        <h2>Phụ kiện</h2>
        <div className="ops-list">
          {accessories.map((item) => (
            <article key={item.id || item.accessoryId}>
              <strong>{item.name || item.accessoryName}</strong>
              <span>{item.price || item.status}</span>
            </article>
          ))}
        </div>
      </section>
    </div>
  );
}
