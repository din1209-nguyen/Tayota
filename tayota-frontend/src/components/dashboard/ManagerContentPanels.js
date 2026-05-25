"use client";

import { useCallback, useEffect, useState } from "react";
import { getCarStylesWithVersions } from "@/lib/services/car";
import { formatVnd, roleLabel, statusLabel, unwrapList } from "@/lib/format";
import {
  attachAccessoryToVehicle, createAccessory, createArticle, createDealership, createVehicle, deactivateDealership,
  detachAccessoryFromVehicle,
  addVehicleGallery, deleteVehicleGallery, getManagerAccessories, getManagerArticles, getManagerDealerships, getManagerUserProfile,
  getManagerUsers, getManagerUserStats, getManagerVehicles, hideAccessory, hideArticle,
  hideVehicle, getVehicleContent, saveVehiclePrice, saveVehicleSpecification, updateAccessory, updateArticle,
  updateDealership, updateManagerUserProfile, updateVehicle, updateVehicleGallery,
} from "@/lib/services/manager";

const LOWER_ROLES = ["SERVICE_ADVISOR", "ASSISTANT", "MECHANIC", "USER"];
const EMPTY_VEHICLE_SPEC = {
  origin: "", fuel: "", numberOfSeats: "", length: "", width: "", height: "",
  capacity: "", cylinderCapacity: "", cylinder: "", gearbox: "", maximumSpeed: "",
  acceleration: "", torque: "", grossWeightAllowance: "", trademarks: "",
};

function PanelState({ loading, error, empty, children }) {
  if (loading) return <div className="status-box">Đang tải dữ liệu...</div>;
  if (error) return <div className="status-box">{error}</div>;
  if (empty) return <div className="status-box">Chưa có dữ liệu.</div>;
  return children;
}

function Feedback({ message }) {
  return message ? <div className="status-box manager-feedback" aria-live="polite">{message}</div> : null;
}

export default function ManagerContentPanels({ tab }) {
  if (tab === "vehicles") return <VehiclePanel />;
  if (tab === "articles") return <ArticlePanel />;
  if (tab === "dealerships") return <DealershipPanel />;
  if (tab === "accessories") return <AccessoryPanel />;
  return <UserPanel />;
}

function VehiclePanel() {
  const empty = { carSeriesId: "", name: "", salePercent: "0", modelYear: new Date().getFullYear(), videoUrl: "", visible: true };
  const [vehicles, setVehicles] = useState([]);
  const [series, setSeries] = useState([]);
  const [form, setForm] = useState(empty);
  const [editing, setEditing] = useState("");
  const [state, setState] = useState({ loading: true, error: "", message: "", busy: false });

  async function load() {
    setState((value) => ({ ...value, loading: true, error: "" }));
    try {
      const [vehicleData, styleData] = await Promise.all([getManagerVehicles(), getCarStylesWithVersions()]);
      setVehicles(unwrapList(vehicleData));
      setSeries(unwrapList(styleData).flatMap((style) => (style.series || style.carSeries || []).map((item) => ({ ...item, styleName: style.name }))));
      setState((value) => ({ ...value, loading: false }));
    } catch (error) {
      setState((value) => ({ ...value, loading: false, error: error.message }));
    }
  }

  useEffect(() => { load(); }, []);

  function edit(vehicle) {
    setEditing(vehicle.id);
    setForm({
      carSeriesId: vehicle.carSeriesId || "",
      name: vehicle.name || "",
      salePercent: vehicle.salePercent ?? "0",
      modelYear: vehicle.modelYear || new Date().getFullYear(),
      videoUrl: vehicle.videoUrl || "",
      visible: vehicle.visible !== false,
    });
  }

  async function submit(event) {
    event.preventDefault();
    setState((value) => ({ ...value, busy: true, message: "" }));
    try {
      const payload = { ...form, modelYear: Number(form.modelYear), salePercent: Number(form.salePercent) };
      if (editing) await updateVehicle(editing, payload);
      else await createVehicle(payload);
      setEditing("");
      setForm(empty);
      setState((value) => ({ ...value, busy: false, message: "Đã lưu thông tin phiên bản xe." }));
      await load();
    } catch (error) {
      setState((value) => ({ ...value, busy: false, message: error.message }));
    }
  }

  async function hide(id) {
    setState((value) => ({ ...value, busy: true, message: "" }));
    try {
      await hideVehicle(id);
      setState((value) => ({ ...value, busy: false, message: "Đã ẩn xe khỏi website." }));
      await load();
    } catch (error) {
      setState((value) => ({ ...value, busy: false, message: error.message }));
    }
  }

  return (
    <div className="manager-content-grid">
      <section className="ops-panel">
        <div className="ops-panel-head"><div><p className="eyebrow">Catalog</p><h2>Danh sách xe</h2></div></div>
        <Feedback message={state.message} />
        <PanelState loading={state.loading} error={state.error} empty={!vehicles.length}>
          <div className="manager-table-list">
            {vehicles.map((vehicle) => (
              <article key={vehicle.id}>
                <div><strong>{vehicle.name}</strong><small>{vehicle.carSeriesName} · {vehicle.modelYear} · {formatVnd(vehicle.minPrice)}</small></div>
                <span className={`status-pill ${vehicle.visible ? "connected" : "error"}`}>{vehicle.visible ? "Đang hiển thị" : "Đã ẩn"}</span>
                <div className="row-actions">
                  <button className="btn btn-ghost" type="button" onClick={() => edit(vehicle)}>Sửa</button>
                  {vehicle.visible ? <button className="btn btn-ghost" disabled={state.busy} type="button" onClick={() => hide(vehicle.id)}>Ẩn</button> : null}
                </div>
              </article>
            ))}
          </div>
        </PanelState>
      </section>
      <section className="ops-panel">
        <div className="ops-panel-head"><div><p className="eyebrow">Phiên bản xe</p><h2>{editing ? "Sửa nội dung" : "Thêm phiên bản"}</h2></div></div>
        <form className="ops-form" onSubmit={submit}>
          <select className="field" required value={form.carSeriesId} onChange={(event) => setForm({ ...form, carSeriesId: event.target.value })}>
            <option value="">Chọn dòng xe</option>
            {series.map((item) => <option key={item.id} value={item.id}>{item.styleName} / {item.name}</option>)}
          </select>
          <input className="field" required placeholder="Tên phiên bản" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} />
          <div className="manager-form-row">
            <input className="field" required min="1900" type="number" value={form.modelYear} onChange={(event) => setForm({ ...form, modelYear: event.target.value })} />
            <input className="field" min="0" step="0.01" type="number" value={form.salePercent} onChange={(event) => setForm({ ...form, salePercent: event.target.value })} placeholder="Giảm giá %" />
          </div>
          <input className="field" placeholder="URL video" value={form.videoUrl} onChange={(event) => setForm({ ...form, videoUrl: event.target.value })} />
          <label className="manager-checkbox"><input type="checkbox" checked={form.visible} onChange={(event) => setForm({ ...form, visible: event.target.checked })} /> Hiển thị trên website</label>
          <div className="row-actions">
            <button className="btn btn-primary" disabled={state.busy} type="submit">{state.busy ? "Đang lưu..." : "Lưu xe"}</button>
            {editing ? <button className="btn btn-ghost" type="button" onClick={() => { setEditing(""); setForm(empty); }}>Hủy</button> : null}
          </div>
          <p className="muted-text">Giá theo màu, gallery và thông số kỹ thuật được quản lý theo API chi tiết của phiên bản xe.</p>
        </form>
        {editing && form.visible ? <VehicleDetailTools vehicleId={editing} /> : null}
      </section>
    </div>
  );
}

function VehicleDetailTools({ vehicleId }) {
  const [detail, setDetail] = useState(null);
  const [specification, setSpecification] = useState(EMPTY_VEHICLE_SPEC);
  const [galleryUrl, setGalleryUrl] = useState("");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      const content = await getVehicleContent(vehicleId);
      setDetail(content);
      setSpecification({ ...EMPTY_VEHICLE_SPEC, ...(content.specification || {}) });
    } catch (error) {
      setMessage(error.message);
    }
  }, [vehicleId]);

  useEffect(() => { load(); }, [load]);

  function numberOrNull(value) {
    return value === "" || value === null ? null : Number(value);
  }

  async function submitSpecification(event) {
    event.preventDefault();
    setBusy(true);
    try {
      await saveVehicleSpecification(vehicleId, {
        ...specification,
        numberOfSeats: numberOrNull(specification.numberOfSeats),
        length: numberOrNull(specification.length),
        width: numberOrNull(specification.width),
        height: numberOrNull(specification.height),
        capacity: numberOrNull(specification.capacity),
        cylinder: numberOrNull(specification.cylinder),
        maximumSpeed: numberOrNull(specification.maximumSpeed),
        grossWeightAllowance: numberOrNull(specification.grossWeightAllowance),
      });
      setMessage("Đã lưu thông số kỹ thuật.");
      await load();
    } catch (error) { setMessage(error.message); } finally { setBusy(false); }
  }

  async function updatePrice(price, nextPrice) {
    setBusy(true);
    try {
      await saveVehiclePrice(vehicleId, {
        exteriorColorId: price.exteriorColorId,
        interiorColorId: price.interiorColorId,
        price: Number(nextPrice),
        exImageUrl: price.exImageUrl,
        inImageUrl: price.inImageUrl,
      });
      setMessage("Đã cập nhật giá theo màu.");
      await load();
    } catch (error) { setMessage(error.message); } finally { setBusy(false); }
  }

  async function addGallery(event) {
    event.preventDefault();
    setBusy(true);
    try {
      await addVehicleGallery(vehicleId, { imageUrl: galleryUrl });
      setGalleryUrl("");
      setMessage("Đã thêm ảnh gallery.");
      await load();
    } catch (error) { setMessage(error.message); } finally { setBusy(false); }
  }

  async function replaceGallery(gallery) {
    const nextUrl = window.prompt("URL hình ảnh mới", gallery.imageUrl);
    if (!nextUrl || nextUrl === gallery.imageUrl) return;
    await updateVehicleGallery(vehicleId, gallery.id, { imageUrl: nextUrl }).then(load).catch((error) => setMessage(error.message));
  }

  async function removeGallery(galleryId) {
    await deleteVehicleGallery(vehicleId, galleryId).then(load).catch((error) => setMessage(error.message));
  }

  if (!detail) return <div className="status-box">{message || "Đang tải nội dung chi tiết..."}</div>;
  return (
    <div className="manager-vehicle-detail-tools">
      <Feedback message={message} />
      <form className="ops-form" onSubmit={submitSpecification}>
        <h3>Thông số kỹ thuật</h3>
        <div className="manager-form-row">
          <input className="field" required value={specification.origin} onChange={(event) => setSpecification({ ...specification, origin: event.target.value })} placeholder="Xuất xứ" />
          <input className="field" required value={specification.fuel} onChange={(event) => setSpecification({ ...specification, fuel: event.target.value })} placeholder="Nhiên liệu" />
          <input className="field" required type="number" value={specification.numberOfSeats} onChange={(event) => setSpecification({ ...specification, numberOfSeats: event.target.value })} placeholder="Số ghế" />
          <input className="field" required type="number" value={specification.length} onChange={(event) => setSpecification({ ...specification, length: event.target.value })} placeholder="Dài (mm)" />
          <input className="field" required type="number" value={specification.width} onChange={(event) => setSpecification({ ...specification, width: event.target.value })} placeholder="Rộng (mm)" />
          <input className="field" required type="number" value={specification.height} onChange={(event) => setSpecification({ ...specification, height: event.target.value })} placeholder="Cao (mm)" />
        </div>
        <button className="btn btn-secondary" disabled={busy} type="submit">Lưu thông số</button>
      </form>
      <div className="manager-detail-block">
        <h3>Giá theo màu</h3>
        {(detail.prices || []).map((price) => (
          <label className="manager-price-row" key={`${price.exteriorColorId}-${price.interiorColorId}`}>
            <span>{price.exteriorColorName} / {price.interiorColorName}</span>
            <input
              className="field"
              defaultValue={price.price}
              type="number"
              onBlur={(event) => Number(event.target.value) !== Number(price.price) && updatePrice(price, event.target.value)}
            />
          </label>
        ))}
        {!detail.prices?.length ? <p className="muted-text">Chưa cấu hình tổ hợp màu/giá.</p> : null}
      </div>
      <div className="manager-detail-block">
        <h3>Gallery</h3>
        <form className="manager-filter-row" onSubmit={addGallery}>
          <input className="field" required value={galleryUrl} onChange={(event) => setGalleryUrl(event.target.value)} placeholder="URL hình ảnh mới" />
          <button className="btn btn-secondary" disabled={busy} type="submit">Thêm ảnh</button>
        </form>
        <div className="manager-gallery-list">
          {(detail.galleries || []).map((gallery) => (
            <article key={gallery.id}>
              <span>{gallery.imageUrl}</span>
              <button className="btn btn-ghost" type="button" onClick={() => replaceGallery(gallery)}>Sửa</button>
              <button className="btn btn-ghost" type="button" onClick={() => removeGallery(gallery.id)}>Xóa</button>
            </article>
          ))}
        </div>
      </div>
    </div>
  );
}

function ArticlePanel() {
  const empty = { type: "NEWS", title: "", content: "", imageUrl: "", carVersionId: "", published: true };
  const [items, setItems] = useState([]);
  const [vehicles, setVehicles] = useState([]);
  const [form, setForm] = useState(empty);
  const [editing, setEditing] = useState("");
  const [state, setState] = useState({ loading: true, error: "", message: "", busy: false });

  async function load() {
    try {
      const [articles, vehicleData] = await Promise.all([getManagerArticles(), getManagerVehicles()]);
      setItems(unwrapList(articles));
      setVehicles(unwrapList(vehicleData));
      setState((value) => ({ ...value, loading: false, error: "" }));
    } catch (error) { setState((value) => ({ ...value, loading: false, error: error.message })); }
  }
  useEffect(() => { load(); }, []);

  async function submit(event) {
    event.preventDefault();
    setState((value) => ({ ...value, busy: true, message: "" }));
    try {
      if (editing) await updateArticle(editing, form); else await createArticle(form);
      setEditing(""); setForm(empty);
      setState((value) => ({ ...value, busy: false, message: "Đã lưu bài viết." }));
      await load();
    } catch (error) { setState((value) => ({ ...value, busy: false, message: error.message })); }
  }

  async function hide(id) {
    await hideArticle(id).then(load).catch((error) => setState((value) => ({ ...value, message: error.message })));
  }

  return (
    <div className="manager-content-grid">
      <section className="ops-panel">
        <div className="ops-panel-head"><div><p className="eyebrow">Tin tức</p><h2>Bài viết website</h2></div></div>
        <Feedback message={state.message} />
        <PanelState loading={state.loading} error={state.error} empty={!items.length}>
          <div className="manager-table-list">
            {items.map((item) => (
              <article key={item.id}>
                <div><strong>{item.title}</strong><small>{item.carVersionId ? "Bài viết xe" : "Tin tức chung"} · {item.type}</small></div>
                <span className={`status-pill ${item.published ? "connected" : "error"}`}>{item.published ? "Đã xuất bản" : "Đã ẩn"}</span>
                <div className="row-actions">
                  <button className="btn btn-ghost" type="button" onClick={() => { setEditing(item.id); setForm({ ...empty, ...item, carVersionId: item.carVersionId || "" }); }}>Sửa</button>
                  {item.published ? <button className="btn btn-ghost" type="button" onClick={() => hide(item.id)}>Ẩn</button> : null}
                </div>
              </article>
            ))}
          </div>
        </PanelState>
      </section>
      <section className="ops-panel">
        <div className="ops-panel-head"><div><p className="eyebrow">Biên tập</p><h2>{editing ? "Sửa bài viết" : "Thêm bài viết"}</h2></div></div>
        <form className="ops-form" onSubmit={submit}>
          <div className="manager-form-row">
            <input className="field" required value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value })} placeholder="Loại bài viết" />
            <select className="field" value={form.carVersionId} onChange={(event) => setForm({ ...form, carVersionId: event.target.value })}>
              <option value="">Tin tức chung</option>
              {vehicles.map((vehicle) => <option key={vehicle.id} value={vehicle.id}>{vehicle.name}</option>)}
            </select>
          </div>
          <input className="field" required value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} placeholder="Tiêu đề" />
          <input className="field" value={form.imageUrl} onChange={(event) => setForm({ ...form, imageUrl: event.target.value })} placeholder="URL ảnh" />
          <textarea className="field manager-textarea" required value={form.content} onChange={(event) => setForm({ ...form, content: event.target.value })} placeholder="Nội dung bài viết" />
          <label className="manager-checkbox"><input type="checkbox" checked={form.published} onChange={(event) => setForm({ ...form, published: event.target.checked })} /> Xuất bản công khai</label>
          <button className="btn btn-primary" disabled={state.busy} type="submit">{state.busy ? "Đang lưu..." : "Lưu bài viết"}</button>
        </form>
      </section>
    </div>
  );
}

function DealershipPanel() {
  const empty = { name: "", address: "", latitude: "", longitude: "", placeId: "", phone: "", operatingHours: "", active: true };
  const [items, setItems] = useState([]);
  const [form, setForm] = useState(empty);
  const [editing, setEditing] = useState("");
  const [state, setState] = useState({ loading: true, error: "", message: "", busy: false });

  async function load() {
    try {
      setItems(unwrapList(await getManagerDealerships()));
      setState((value) => ({ ...value, loading: false, error: "" }));
    } catch (error) { setState((value) => ({ ...value, loading: false, error: error.message })); }
  }
  useEffect(() => { load(); }, []);

  async function submit(event) {
    event.preventDefault();
    setState((value) => ({ ...value, busy: true, message: "" }));
    try {
      const payload = { ...form, latitude: Number(form.latitude), longitude: Number(form.longitude) };
      if (editing) await updateDealership(editing, payload); else await createDealership(payload);
      setEditing(""); setForm(empty);
      setState((value) => ({ ...value, busy: false, message: "Đã lưu thông tin đại lý." }));
      await load();
    } catch (error) { setState((value) => ({ ...value, busy: false, message: error.message })); }
  }

  async function deactivate(id) {
    try {
      await deactivateDealership(id);
      setState((value) => ({ ...value, message: "Đại lý đã ngừng hiển thị công khai." }));
      await load();
    } catch (error) { setState((value) => ({ ...value, message: error.message })); }
  }

  return (
    <div className="manager-content-grid">
      <section className="ops-panel">
        <div className="ops-panel-head"><div><p className="eyebrow">Điểm bán</p><h2>Danh sách đại lý</h2></div></div>
        <Feedback message={state.message} />
        <PanelState loading={state.loading} error={state.error} empty={!items.length}>
          <div className="manager-table-list">
            {items.map((item) => (
              <article key={item.id}>
                <div><strong>{item.name}</strong><small>{item.address}</small></div>
                <span className={`status-pill ${item.active ? "connected" : "error"}`}>{item.active ? "Hoạt động" : "Ngừng hoạt động"}</span>
                <div className="row-actions">
                  <button className="btn btn-ghost" type="button" onClick={() => { setEditing(item.id); setForm({ ...empty, ...item }); }}>Sửa</button>
                  {item.active ? <button className="btn btn-ghost" type="button" onClick={() => deactivate(item.id)}>Ngừng</button> : null}
                </div>
              </article>
            ))}
          </div>
        </PanelState>
      </section>
      <section className="ops-panel">
        <div className="ops-panel-head"><div><p className="eyebrow">Đại lý</p><h2>{editing ? "Sửa thông tin" : "Thêm đại lý"}</h2></div></div>
        <form className="ops-form" onSubmit={submit}>
          <input className="field" required value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} placeholder="Tên đại lý" />
          <input className="field" required value={form.address} onChange={(event) => setForm({ ...form, address: event.target.value })} placeholder="Địa chỉ" />
          <div className="manager-form-row">
            <input className="field" required type="number" step="any" value={form.latitude} onChange={(event) => setForm({ ...form, latitude: event.target.value })} placeholder="Vĩ độ" />
            <input className="field" required type="number" step="any" value={form.longitude} onChange={(event) => setForm({ ...form, longitude: event.target.value })} placeholder="Kinh độ" />
          </div>
          <div className="manager-form-row">
            <input className="field" value={form.phone || ""} onChange={(event) => setForm({ ...form, phone: event.target.value })} placeholder="Điện thoại" />
            <input className="field" value={form.operatingHours || ""} onChange={(event) => setForm({ ...form, operatingHours: event.target.value })} placeholder="Giờ hoạt động" />
          </div>
          <input className="field" value={form.placeId || ""} onChange={(event) => setForm({ ...form, placeId: event.target.value })} placeholder="Google Place ID" />
          <label className="manager-checkbox"><input type="checkbox" checked={form.active} onChange={(event) => setForm({ ...form, active: event.target.checked })} /> Đang hoạt động</label>
          <button className="btn btn-primary" disabled={state.busy} type="submit">{state.busy ? "Đang lưu..." : "Lưu đại lý"}</button>
        </form>
      </section>
    </div>
  );
}

function AccessoryPanel() {
  const empty = { model: "", brand: "", price: "", description: "", useContent: "", reminderContent: "", type: "", visible: true };
  const [items, setItems] = useState([]);
  const [vehicles, setVehicles] = useState([]);
  const [linkedVehicleId, setLinkedVehicleId] = useState("");
  const [form, setForm] = useState(empty);
  const [editing, setEditing] = useState("");
  const [state, setState] = useState({ loading: true, error: "", message: "", busy: false });

  async function load() {
    try {
      const [result, vehicleData] = await Promise.all([getManagerAccessories({ page: 0, size: 50 }), getManagerVehicles()]);
      setItems(unwrapList(result));
      setVehicles(unwrapList(vehicleData));
      setState((value) => ({ ...value, loading: false, error: "" }));
    } catch (error) { setState((value) => ({ ...value, loading: false, error: error.message })); }
  }
  useEffect(() => { load(); }, []);

  async function submit(event) {
    event.preventDefault();
    setState((value) => ({ ...value, busy: true, message: "" }));
    try {
      const payload = { ...form, price: Number(form.price) };
      if (editing) await updateAccessory(editing, payload); else await createAccessory(payload);
      setEditing(""); setForm(empty);
      setState((value) => ({ ...value, busy: false, message: "Đã lưu phụ kiện." }));
      await load();
    } catch (error) { setState((value) => ({ ...value, busy: false, message: error.message })); }
  }

  async function hide(id) {
    try {
      await hideAccessory(id);
      setState((value) => ({ ...value, message: "Đã ẩn phụ kiện khỏi website." }));
      await load();
    } catch (error) { setState((value) => ({ ...value, message: error.message })); }
  }

  async function updateLink(action) {
    if (!editing || !linkedVehicleId) return;
    try {
      if (action === "attach") await attachAccessoryToVehicle(editing, linkedVehicleId);
      else await detachAccessoryFromVehicle(editing, linkedVehicleId);
      setState((value) => ({ ...value, message: action === "attach" ? "Đã gắn phụ kiện cho xe." : "Đã gỡ phụ kiện khỏi xe." }));
    } catch (error) { setState((value) => ({ ...value, message: error.message })); }
  }

  return (
    <div className="manager-content-grid">
      <section className="ops-panel">
        <div className="ops-panel-head"><div><p className="eyebrow">Catalog</p><h2>Phụ kiện</h2></div></div>
        <Feedback message={state.message} />
        <PanelState loading={state.loading} error={state.error} empty={!items.length}>
          <div className="manager-table-list">
            {items.map((item) => (
              <article key={item.id}>
                <div><strong>{item.model}</strong><small>{item.type} · {formatVnd(item.price)}</small></div>
                <span className={`status-pill ${item.visible ? "connected" : "error"}`}>{item.visible ? "Hiển thị" : "Đã ẩn"}</span>
                <div className="row-actions">
                  <button className="btn btn-ghost" type="button" onClick={() => { setEditing(item.id); setForm({ ...empty, ...item }); }}>Sửa</button>
                  {item.visible ? <button className="btn btn-ghost" type="button" onClick={() => hide(item.id)}>Ẩn</button> : null}
                </div>
              </article>
            ))}
          </div>
        </PanelState>
      </section>
      <section className="ops-panel">
        <div className="ops-panel-head"><div><p className="eyebrow">Phụ kiện</p><h2>{editing ? "Sửa phụ kiện" : "Thêm phụ kiện"}</h2></div></div>
        <form className="ops-form" onSubmit={submit}>
          <div className="manager-form-row">
            <input className="field" required value={form.model} onChange={(event) => setForm({ ...form, model: event.target.value })} placeholder="Tên/model" />
            <input className="field" required value={form.brand} onChange={(event) => setForm({ ...form, brand: event.target.value })} placeholder="Thương hiệu" />
          </div>
          <div className="manager-form-row">
            <input className="field" required value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value })} placeholder="Loại" />
            <input className="field" required min="0" type="number" value={form.price} onChange={(event) => setForm({ ...form, price: event.target.value })} placeholder="Giá bán" />
          </div>
          <textarea className="field" required value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} placeholder="Mô tả" />
          <textarea className="field" required value={form.useContent} onChange={(event) => setForm({ ...form, useContent: event.target.value })} placeholder="Công dụng" />
          <textarea className="field" required value={form.reminderContent} onChange={(event) => setForm({ ...form, reminderContent: event.target.value })} placeholder="Lưu ý" />
          <label className="manager-checkbox"><input type="checkbox" checked={form.visible} onChange={(event) => setForm({ ...form, visible: event.target.checked })} /> Hiển thị công khai</label>
          <button className="btn btn-primary" disabled={state.busy} type="submit">{state.busy ? "Đang lưu..." : "Lưu phụ kiện"}</button>
          {editing ? (
            <div className="manager-detail-block">
              <h3>Phiên bản xe tương thích</h3>
              <select className="field" value={linkedVehicleId} onChange={(event) => setLinkedVehicleId(event.target.value)}>
                <option value="">Chọn phiên bản xe</option>
                {vehicles.map((vehicle) => <option key={vehicle.id} value={vehicle.id}>{vehicle.name}</option>)}
              </select>
              <div className="row-actions">
                <button className="btn btn-secondary" disabled={!linkedVehicleId} type="button" onClick={() => updateLink("attach")}>Gắn xe</button>
                <button className="btn btn-ghost" disabled={!linkedVehicleId} type="button" onClick={() => updateLink("detach")}>Gỡ xe</button>
              </div>
            </div>
          ) : null}
          <p className="muted-text">Phụ kiện là nội dung catalog; không theo dõi số lượng tồn tại từng đại lý.</p>
        </form>
      </section>
    </div>
  );
}

function UserPanel() {
  const [items, setItems] = useState([]);
  const [stats, setStats] = useState(null);
  const [role, setRole] = useState("");
  const [keyword, setKeyword] = useState("");
  const [profile, setProfile] = useState(null);
  const [state, setState] = useState({ loading: true, error: "", message: "", busy: false });

  async function load() {
    try {
      const [users, nextStats] = await Promise.all([getManagerUsers({ keyword, role, page: 0, size: 50 }), getManagerUserStats()]);
      setItems(unwrapList(users));
      setStats(nextStats);
      setState((value) => ({ ...value, loading: false, error: "" }));
    } catch (error) { setState((value) => ({ ...value, loading: false, error: error.message })); }
  }
  useEffect(() => {
    Promise.all([getManagerUsers({ page: 0, size: 50 }), getManagerUserStats()])
      .then(([users, nextStats]) => {
        setItems(unwrapList(users));
        setStats(nextStats);
        setState((value) => ({ ...value, loading: false, error: "" }));
      })
      .catch((error) => setState((value) => ({ ...value, loading: false, error: error.message })));
  }, []);
  const activeCount = stats?.byStatus?.ACTIVE || 0;

  async function selectUser(user) {
    try {
      const result = await getManagerUserProfile(user.id);
      setProfile(result);
    } catch (error) { setState((value) => ({ ...value, message: error.message })); }
  }

  async function submitProfile(event) {
    event.preventDefault();
    setState((value) => ({ ...value, busy: true, message: "" }));
    try {
      await updateManagerUserProfile(profile);
      setState((value) => ({ ...value, busy: false, message: "Đã cập nhật hồ sơ người dùng." }));
      await load();
    } catch (error) { setState((value) => ({ ...value, busy: false, message: error.message })); }
  }

  return (
    <div className="manager-users">
      <div className="manager-stat-grid">
        <article><span>Tổng người dùng cấp dưới</span><strong>{stats?.total || 0}</strong></article>
        <article><span>Đang hoạt động</span><strong>{activeCount}</strong></article>
        {LOWER_ROLES.map((item) => <article key={item}><span>{roleLabel(item)}</span><strong>{stats?.byRole?.[item] || 0}</strong></article>)}
      </div>
      <div className="manager-content-grid">
        <section className="ops-panel">
          <div className="ops-panel-head"><div><p className="eyebrow">Hồ sơ</p><h2>Người dùng cấp dưới</h2></div></div>
          <Feedback message={state.message} />
          <form className="manager-filter-row" onSubmit={(event) => { event.preventDefault(); load(); }}>
            <input className="field" value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="Tìm email, tên, điện thoại" />
            <select className="field" value={role} onChange={(event) => setRole(event.target.value)}>
              <option value="">Tất cả role</option>
              {LOWER_ROLES.map((item) => <option key={item} value={item}>{roleLabel(item)}</option>)}
            </select>
            <button className="btn btn-secondary" type="submit">Lọc</button>
          </form>
          <PanelState loading={state.loading} error={state.error} empty={!items.length}>
            <div className="manager-table-list">
              {items.map((user) => (
                <article key={user.id}>
                  <div><strong>{user.fullname || user.email}</strong><small>{user.email} · {roleLabel(user.role)}</small></div>
                  <span className={`status-pill ${user.status === "ACTIVE" ? "connected" : "error"}`}>{statusLabel(user.status)}</span>
                  <button className="btn btn-ghost" type="button" onClick={() => selectUser(user)}>Xem/Sửa</button>
                </article>
              ))}
            </div>
          </PanelState>
        </section>
        <section className="ops-panel">
          <div className="ops-panel-head"><div><p className="eyebrow">Chi tiết</p><h2>Cập nhật hồ sơ</h2></div></div>
          {!profile ? <div className="status-box">Chọn người dùng để xem và cập nhật hồ sơ.</div> : (
            <form className="ops-form" onSubmit={submitProfile}>
              <input className="field" disabled value={profile.email || ""} />
              <input className="field" value={profile.fullname || ""} onChange={(event) => setProfile({ ...profile, fullname: event.target.value })} placeholder="Họ tên" />
              <input className="field" value={profile.phone || ""} onChange={(event) => setProfile({ ...profile, phone: event.target.value })} placeholder="Số điện thoại 10 chữ số" />
              <input className="field" type="date" value={profile.birthDate || ""} onChange={(event) => setProfile({ ...profile, birthDate: event.target.value })} />
              <input className="field" value={profile.address || ""} onChange={(event) => setProfile({ ...profile, address: event.target.value })} placeholder="Địa chỉ" />
              <input className="field" value={profile.avatarUrl || ""} onChange={(event) => setProfile({ ...profile, avatarUrl: event.target.value })} placeholder="URL ảnh đại diện" />
              <button className="btn btn-primary" disabled={state.busy} type="submit">{state.busy ? "Đang lưu..." : "Lưu hồ sơ"}</button>
            </form>
          )}
        </section>
      </div>
    </div>
  );
}
