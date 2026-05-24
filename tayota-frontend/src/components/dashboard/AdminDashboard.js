"use client";

import { useEffect, useState } from "react";
import { apiFetch } from "@/lib/api";
import { getAccessories, getCarVersions } from "@/lib/services/car";
import { unwrapList } from "@/lib/format";

export default function AdminDashboard() {
  const [vehicles, setVehicles] = useState([]);
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

    Promise.all([
      getCarVersions({ page: 0, size: 8 }),
      getAccessories({ page: 0, size: 8 }),
    ])
      .then(([nextVehicles, nextAccessories]) => {
        if (!active) return;
        setVehicles(unwrapList(nextVehicles));
        setAccessories(unwrapList(nextAccessories));
      })
      .catch((error) => {
        if (active) setMessage(error.message);
      });

    return () => {
      active = false;
    };
  }, []);

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
      <section className="ops-panel">
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

      <section className="ops-panel">
        <p className="eyebrow">Catalog</p>
        <h2>Phiên bản xe</h2>
        <div className="ops-list">
          {vehicles.map((vehicle) => (
            <article key={vehicle.id || vehicle.carVersionId}>
              <strong>{vehicle.versionName || vehicle.name || vehicle.carVersionName}</strong>
              <span>{vehicle.status || vehicle.modelYear}</span>
            </article>
          ))}
        </div>
      </section>

      <section className="ops-panel">
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
