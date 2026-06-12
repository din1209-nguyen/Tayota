"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useState } from "react";
import MediaUploadField from "@/components/dashboard/MediaUploadField";
import { getMe } from "@/lib/services/auth";
import { getCarStylesWithVersions } from "@/lib/services/car";
import { uploadMedia } from "@/lib/services/media";
import { formatVnd, roleLabel, statusLabel, unwrapList } from "@/lib/format";
import { getDashboardPath, setCurrentUser } from "@/lib/session";
import {
  addVehicleGallery,
  attachAccessoryToVehicle,
  createAccessory,
  createArticle,
  createDealership,
  createVehicle,
  createVehicleSeries,
  createVehicleStyle,
  deactivateDealership,
  deleteVehicleGallery,
  deleteVehiclePrice,
  detachAccessoryFromVehicle,
  getManagerAccessories,
  getManagerArticles,
  getManagerDealerships,
  getManagerUserProfile,
  getManagerUsers,
  getManagerUserStats,
  getManagerVehicleDetail,
  getManagerVehicles,
  hideAccessory,
  hideArticle,
  hideVehicle,
  saveVehiclePrice,
  saveVehicleSpecification,
  updateAccessory,
  updateArticle,
  updateDealership,
  updateManagerUserProfile,
  updateVehicle,
  updateVehicleGallery,
} from "@/lib/services/manager";

const LOWER_ROLES = ["SERVICE_ADVISOR", "ASSISTANT", "MECHANIC", "USER"];
const LOWER_ROLE_CHART_COLORS = {
  SERVICE_ADVISOR: "#2563eb",
  ASSISTANT: "#d71920",
  MECHANIC: "#16a34a",
  USER: "#f59e0b",
};
const NEW_OPTION_VALUE = "__new__";
const VEHICLE_EDITOR_TABS = [
  ["general", "Thông tin chung"],
  ["specs", "Thông số"],
  ["prices", "Giá & màu"],
  ["articles", "Bài viết"],
  ["gallery", "Gallery"],
];
const EMPTY_VEHICLE_SPEC = {
  origin: "",
  fuel: "",
  numberOfSeats: "",
  length: "",
  width: "",
  height: "",
  capacity: "",
  cylinderCapacity: "",
  cylinder: "",
  gearbox: "",
  maximumSpeed: "",
  acceleration: "",
  torque: "",
  grossWeightAllowance: "",
  trademarks: "",
};
const SPEC_FIELDS = [
  ["origin", "Xuất xứ", "text", true],
  ["fuel", "Nhiên liệu", "text", true],
  ["numberOfSeats", "Số chỗ ngồi", "number", true],
  ["length", "Chiều dài (mm)", "number", true],
  ["width", "Chiều rộng (mm)", "number", true],
  ["height", "Chiều cao (mm)", "number", true],
  ["capacity", "Dung tích bình nhiên liệu (L)", "number", false],
  ["cylinderCapacity", "Dung tích xy-lanh", "text", false],
  ["cylinder", "Số xy-lanh", "number", false],
  ["gearbox", "Hộp số", "text", false],
  ["maximumSpeed", "Tốc độ tối đa (km/h)", "number", false],
  ["acceleration", "Thời gian tăng tốc", "text", false],
  ["torque", "Mô-men xoắn", "text", false],
  ["grossWeightAllowance", "Khối lượng toàn tải (kg)", "number", false],
  ["trademarks", "Thương hiệu", "text", false],
];
const PRICE_RANGE_OPTIONS = [
  { value: "0-600000000", label: "Dưới 600 triệu" },
  { value: "600000000-900000000", label: "600 - 900 triệu" },
  { value: "900000000-1200000000", label: "900 triệu - 1.2 tỷ" },
  { value: "1200000000-", label: "Trên 1.2 tỷ" },
];

function PanelState({ loading, error, empty, children }) {
  if (loading) return <div className="status-box">Đang tải dữ liệu...</div>;
  if (error) return <div className="status-box">{error}</div>;
  if (empty) return <div className="status-box">Chưa có dữ liệu.</div>;
  return children;
}

function Feedback({ message }) {
  return message ? <div className="status-box manager-feedback" aria-live="polite">{message}</div> : null;
}

function toText(value) {
  return value === undefined || value === null ? "" : String(value);
}

function numberOrNull(value) {
  return value === "" || value === null || value === undefined ? null : Number(value);
}

function getVehicleSpecValue(vehicle, keys = []) {
  const spec = vehicle?.specification || {};
  for (const key of keys) {
    const value = vehicle?.[key] || spec?.[key];
    if (value !== undefined && value !== null && value !== "") return value;
  }
  return "";
}

function isPriceInRange(price, range) {
  if (!range) return true;
  const numericPrice = Number(price);
  if (!Number.isFinite(numericPrice) || numericPrice <= 0) return false;
  const [min, max] = range.split("-").map((value) => (value ? Number(value) : null));
  if (min !== null && numericPrice < min) return false;
  if (max !== null && numericPrice > max) return false;
  return true;
}

function uniqueOptions(items = [], getValue) {
  const seen = new Map();
  items.forEach((item) => {
    const value = getValue(item);
    if (value === undefined || value === null || value === "") return;
    const key = String(value);
    if (!seen.has(key)) seen.set(key, { value: key, label: key });
  });
  return Array.from(seen.values()).sort((left, right) => left.label.localeCompare(right.label, "vi", { numeric: true }));
}

export default function ManagerContentPanels({ tab }) {
  if (tab === "vehicles") return <VehiclePanel />;
  if (tab === "articles") return <ArticlePanel />;
  if (tab === "dealerships") return <DealershipPanel />;
  if (tab === "accessories") return <AccessoryPanel />;
  return <UserPanel />;
}

export function ManagerVehicleEditorPage({ vehicleId }) {
  const router = useRouter();
  const isCreate = !vehicleId || vehicleId === "new";
  const [ready, setReady] = useState(false);
  const [styles, setStyles] = useState([]);
  const [series, setSeries] = useState([]);
  const [vehicles, setVehicles] = useState([]);
  const [message, setMessage] = useState("");

  const load = useCallback(async () => {
    try {
      const user = await getMe();
      setCurrentUser(user);
      if (user?.role !== "MANAGER") {
        router.replace(getDashboardPath(user?.role));
        return;
      }
      const [styleData, vehicleData] = await Promise.all([getCarStylesWithVersions(), getManagerVehicles()]);
      const nextStyles = unwrapList(styleData).map((style) => ({ ...style, series: style.series || style.carSeries || [] }));
      setStyles(nextStyles);
      setSeries(nextStyles.flatMap((style) => (style.series || []).map((item) => ({ ...item, styleId: style.id, styleName: style.name }))));
      setVehicles(unwrapList(vehicleData));
      setReady(true);
    } catch (error) {
      setMessage(error.message || "Phiên đăng nhập đã hết hạn.");
      router.replace("/auth/login");
    }
  }, [router]);

  useEffect(() => {
    void Promise.resolve().then(load);
  }, [load]);

  if (!ready) return <div className="status-box">{message || "Đang tải trình chỉnh sửa xe..."}</div>;

  const vehicle = vehicles.find((item) => String(item.id) === String(vehicleId));

  return (
    <VehicleEditorDialog
      mode={isCreate ? "create" : "edit"}
      onClose={() => router.push("/dashboard/manager?tab=vehicles")}
      onSaved={async (savedVehicleId) => {
        if (isCreate && savedVehicleId) router.replace(`/dashboard/manager/vehicles/${savedVehicleId}`);
        else await load();
      }}
      series={series}
      styles={styles}
      vehicle={vehicle}
      vehicleId={isCreate ? "" : vehicleId}
      variant="page"
    />
  );
}

export function ManagerArticleEditorPage({ articleId }) {
  const router = useRouter();
  const isCreate = !articleId || articleId === "new";
  const empty = useMemo(() => ({ type: "NEWS", title: "", content: "", imageUrl: "", carVersionId: "", published: true }), []);
  const [ready, setReady] = useState(false);
  const [vehicles, setVehicles] = useState([]);
  const [form, setForm] = useState(empty);
  const [state, setState] = useState({ message: "", busy: false });

  const load = useCallback(async () => {
    try {
      const user = await getMe();
      setCurrentUser(user);
      if (user?.role !== "MANAGER") {
        router.replace(getDashboardPath(user?.role));
        return;
      }
      const [articles, vehicleData] = await Promise.all([getManagerArticles(), getManagerVehicles()]);
      const nextArticles = unwrapList(articles);
      setVehicles(unwrapList(vehicleData));
      if (!isCreate) {
        const article = nextArticles.find((item) => String(item.id) === String(articleId));
        if (article) setForm({ ...empty, ...article, carVersionId: article.carVersionId || "" });
        else setState((value) => ({ ...value, message: "Không tìm thấy bài viết." }));
      }
      setReady(true);
    } catch (error) {
      setState((value) => ({ ...value, message: error.message || "Phiên đăng nhập đã hết hạn." }));
      router.replace("/auth/login");
    }
  }, [articleId, empty, isCreate, router]);

  useEffect(() => {
    void Promise.resolve().then(load);
  }, [load]);

  async function submit(event) {
    event.preventDefault();
    setState({ message: "", busy: true });
    try {
      if (isCreate) await createArticle(form);
      else await updateArticle(articleId, form);
      router.push("/dashboard/manager?tab=articles");
    } catch (error) {
      setState({ message: error.message, busy: false });
    }
  }

  async function hide() {
    if (isCreate) return;
    setState({ message: "", busy: true });
    try {
      await hideArticle(articleId);
      router.push("/dashboard/manager?tab=articles");
    } catch (error) {
      setState({ message: error.message, busy: false });
    }
  }

  if (!ready) return <div className="status-box">{state.message || "Đang tải trình biên tập bài viết..."}</div>;

  return (
    <div className="manager-editor-page-shell">
      <section className="manager-editor-page">
        <header className="manager-modal-head">
          <div>
            <p className="eyebrow">Biên tập nội dung</p>
            <h2>{isCreate ? "Thêm bài viết" : "Sửa bài viết"}</h2>
          </div>
          <Link className="btn btn-ghost" href="/dashboard/manager?tab=articles">Quay lại</Link>
        </header>
        <div className="manager-modal-body manager-article-editor-layout">
          <Feedback message={state.message} />
          <form className="ops-form manager-editor-main-form" onSubmit={submit}>
            <div className="manager-form-row">
              <input className="field" required value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value })} placeholder="Loại bài viết" />
              <select className="field" value={form.carVersionId || ""} onChange={(event) => setForm({ ...form, carVersionId: event.target.value })}>
                <option value="">Tin tức chung</option>
                {vehicles.map((vehicle) => <option key={vehicle.id} value={vehicle.id}>{vehicle.name}</option>)}
              </select>
            </div>
            <input className="field" required value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} placeholder="Tiêu đề" />
            <MediaUploadField label="Ảnh bài viết" value={form.imageUrl} onChange={(value) => setForm({ ...form, imageUrl: value })} context="ARTICLE_IMAGE" />
            <textarea className="field manager-textarea large" required value={form.content} onChange={(event) => setForm({ ...form, content: event.target.value })} placeholder="Nội dung bài viết" />
            <label className="manager-checkbox"><input type="checkbox" checked={form.published} onChange={(event) => setForm({ ...form, published: event.target.checked })} /> Xuất bản công khai</label>
            <div className="row-actions manager-modal-actions">
              <button className="btn btn-primary" disabled={state.busy} type="submit">{state.busy ? "Đang lưu..." : "Lưu bài viết"}</button>
              {!isCreate && form.published ? <button className="btn btn-ghost" disabled={state.busy} type="button" onClick={hide}>Ẩn bài viết</button> : null}
            </div>
          </form>
          <aside className="manager-editor-preview">
            <span className="manager-preview-image" style={form.imageUrl ? { backgroundImage: `url(${form.imageUrl})` } : undefined} />
            <p className="eyebrow">{form.type || "NEWS"}</p>
            <h3>{form.title || "Tiêu đề bài viết"}</h3>
            <p>{form.content || "Nội dung xem trước sẽ hiển thị tại đây khi bạn nhập bài viết."}</p>
          </aside>
        </div>
      </section>
    </div>
  );
}

function VehiclePanel() {
  const emptyFilters = { styleId: "", priceRange: "", numberOfSeats: "", seriesId: "", versionKeyword: "", fuel: "", origin: "" };
  const [vehicles, setVehicles] = useState([]);
  const [styles, setStyles] = useState([]);
  const [series, setSeries] = useState([]);
  const [filters, setFilters] = useState(emptyFilters);
  const [draftFilters, setDraftFilters] = useState(emptyFilters);
  const [state, setState] = useState({ loading: true, error: "", message: "", busy: false });

  const load = useCallback(async () => {
    setState((value) => ({ ...value, loading: true, error: "" }));
    try {
      const [vehicleData, styleData] = await Promise.all([getManagerVehicles(), getCarStylesWithVersions()]);
      const nextVehicles = unwrapList(vehicleData);
      const nextStyles = unwrapList(styleData).map((style) => ({ ...style, series: style.series || style.carSeries || [] }));
      setVehicles(nextVehicles);
      setStyles(nextStyles);
      setSeries(nextStyles.flatMap((style) => (style.series || []).map((item) => ({ ...item, styleId: style.id, styleName: style.name }))));
      setState((value) => ({ ...value, loading: false }));
    } catch (error) {
      setState((value) => ({ ...value, loading: false, error: error.message }));
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const draftOptionVehicles = vehicles.filter((vehicle) => {
    const styleId = vehicle.carStyleId || series.find((item) => String(item.id) === String(vehicle.carSeriesId))?.styleId;
    const matchesStyle = !draftFilters.styleId || String(styleId) === String(draftFilters.styleId);
    const matchesSeries = !draftFilters.seriesId || String(vehicle.carSeriesId) === String(draftFilters.seriesId);
    return matchesStyle && matchesSeries;
  });
  const filteredVehicles = vehicles.filter((vehicle) => {
    const styleId = vehicle.carStyleId || series.find((item) => String(item.id) === String(vehicle.carSeriesId))?.styleId;
    const seats = getVehicleSpecValue(vehicle, ["numberOfSeats"]);
    const fuel = getVehicleSpecValue(vehicle, ["fuel"]);
    const origin = getVehicleSpecValue(vehicle, ["origin"]);
    const versionName = vehicle.name || "";
    return (!filters.styleId || String(styleId) === String(filters.styleId))
      && (!filters.seriesId || String(vehicle.carSeriesId) === String(filters.seriesId))
      && (!filters.numberOfSeats || String(seats) === String(filters.numberOfSeats))
      && (!filters.versionKeyword || versionName === filters.versionKeyword)
      && (!filters.fuel || String(fuel).toLowerCase() === String(filters.fuel).toLowerCase())
      && (!filters.origin || String(origin).toLowerCase() === String(filters.origin).toLowerCase())
      && isPriceInRange(vehicle.minPrice, filters.priceRange);
  });
  const draftSeriesOptions = draftFilters.styleId ? series.filter((item) => String(item.styleId) === String(draftFilters.styleId)) : series;

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
    <div className="manager-content-grid manager-vehicle-layout list-only">
      <section className="ops-panel">
        <div className="ops-panel-head">
          <div>
            <p className="eyebrow">Catalog</p>
            <h2>Danh sách xe</h2>
          </div>
          <Link className="btn btn-primary" href="/dashboard/manager/vehicles/new">Thêm xe</Link>
        </div>
        <Feedback message={state.message} />
        <form className="filter-panel catalog-filter manager-catalog-filter" onSubmit={(event) => { event.preventDefault(); setFilters(draftFilters); }}>
          <div className="catalog-filter-head">
            <div>
              <p className="eyebrow">Bộ lọc</p>
              <h2>Tìm phiên bản xe</h2>
            </div>
            <button className="filter-reset" type="button" onClick={() => { setDraftFilters(emptyFilters); setFilters(emptyFilters); }}>Xóa lọc</button>
          </div>
          <div className="catalog-filter-grid">
            <SelectField label="Kiểu dáng" value={draftFilters.styleId} onChange={(value) => setDraftFilters({ ...draftFilters, styleId: value, seriesId: "", numberOfSeats: "", versionKeyword: "", fuel: "", origin: "" })} options={styles.map((style) => ({ value: style.id, label: style.name }))} />
            <SelectField label="Giá" value={draftFilters.priceRange} onChange={(value) => setDraftFilters({ ...draftFilters, priceRange: value })} options={PRICE_RANGE_OPTIONS} />
            <SelectField label="Số chỗ" value={draftFilters.numberOfSeats} onChange={(value) => setDraftFilters({ ...draftFilters, numberOfSeats: value })} options={uniqueOptions(draftOptionVehicles, (vehicle) => getVehicleSpecValue(vehicle, ["numberOfSeats"]))} />
            <SelectField label="Dòng xe" value={draftFilters.seriesId} onChange={(value) => setDraftFilters({ ...draftFilters, seriesId: value, numberOfSeats: "", versionKeyword: "", fuel: "", origin: "" })} options={draftSeriesOptions.map((item) => ({ value: item.id, label: item.name }))} />
            <SelectField label="Phiên bản" value={draftFilters.versionKeyword} onChange={(value) => setDraftFilters({ ...draftFilters, versionKeyword: value })} options={uniqueOptions(draftOptionVehicles, (vehicle) => vehicle.name)} />
            <SelectField label="Nhiên liệu" value={draftFilters.fuel} onChange={(value) => setDraftFilters({ ...draftFilters, fuel: value })} options={uniqueOptions(draftOptionVehicles, (vehicle) => getVehicleSpecValue(vehicle, ["fuel"]))} />
            <SelectField label="Xuất xứ" value={draftFilters.origin} onChange={(value) => setDraftFilters({ ...draftFilters, origin: value })} options={uniqueOptions(draftOptionVehicles, (vehicle) => getVehicleSpecValue(vehicle, ["origin"]))} />
          </div>
          <div className="catalog-filter-actions manager-filter-actions">
            <button className="btn btn-primary" type="submit">Áp dụng</button>
          </div>
        </form>
        <PanelState loading={state.loading} error={state.error} empty={!vehicles.length}>
          {!filteredVehicles.length ? <div className="status-box">Không có xe phù hợp với bộ lọc.</div> : null}
          <div className="manager-vehicle-list">
            {filteredVehicles.map((vehicle) => (
              <article key={vehicle.id}>
                <span className="manager-vehicle-thumb" style={vehicle.imageUrl ? { backgroundImage: `url(${vehicle.imageUrl})` } : undefined} />
                <div className="manager-vehicle-main">
                  <strong>{vehicle.name}</strong>
                  <small>{vehicle.carStyleName || "Chưa có kiểu dáng"} · {vehicle.carSeriesName || "Chưa có dòng"} · {vehicle.modelYear}</small>
                  <span>{formatVnd(vehicle.minPrice)} · {getVehicleSpecValue(vehicle, ["fuel"]) || "Chưa có nhiên liệu"} · {getVehicleSpecValue(vehicle, ["numberOfSeats"]) || "?"} chỗ</span>
                </div>
                <span className={`status-pill ${vehicle.visible ? "connected" : "error"}`}>{vehicle.visible ? "Đang hiển thị" : "Đã ẩn"}</span>
                <div className="row-actions">
                  <Link className="btn btn-ghost" href={`/dashboard/manager/vehicles/${vehicle.id}`}>Chi tiết</Link>
                  {vehicle.visible ? <button className="btn btn-ghost" disabled={state.busy} type="button" onClick={() => hide(vehicle.id)}>Ẩn</button> : null}
                </div>
              </article>
            ))}
          </div>
        </PanelState>
      </section>
    </div>
  );
}
function SelectField({ label, value, onChange, options }) {
  return (
    <label className="label catalog-select-field">
      {label}
      <select className="field" value={value} onChange={(event) => onChange(event.target.value)}>
        <option value="">Tất cả</option>
        {options.map((option) => <option key={option.value || "empty"} value={option.value}>{option.label}</option>)}
      </select>
    </label>
  );
}

export function VehicleEditorDialog({ mode, onClose, onSaved, series, styles, vehicle, vehicleId, variant = "modal" }) {
  const isCreate = mode === "create";
  const emptyForm = useMemo(() => ({
    carStyleId: "",
    carStyleName: "",
    carSeriesId: "",
    carSeriesName: "",
    name: "",
    salePercent: "0",
    modelYear: new Date().getFullYear(),
    imageUrl: "",
    videoUrl: "",
    visible: true,
  }), []);
  const [activeTab, setActiveTab] = useState("general");
  const [detail, setDetail] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [specification, setSpecification] = useState(EMPTY_VEHICLE_SPEC);
  const [priceForm, setPriceForm] = useState({ exteriorColorName: "", interiorColorName: "", price: "", exImageUrl: "", inImageUrl: "" });
  const [articleForm, setArticleForm] = useState({ id: "", type: "FEATURE", title: "", content: "", imageUrl: "", published: true });
  const [galleryUrl, setGalleryUrl] = useState("");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);

  const availableSeries = form.carStyleId ? series.filter((item) => String(item.styleId) === String(form.carStyleId)) : series;
  const isCreatingStyle = isCreate && form.carStyleId === NEW_OPTION_VALUE;
  const isCreatingSeries = isCreate && (form.carSeriesId === NEW_OPTION_VALUE || isCreatingStyle);

  const loadDetail = useCallback(async () => {
    if (!vehicleId) return;
    setMessage("");
    try {
      const content = await getManagerVehicleDetail(vehicleId);
      setDetail(content);
      setSpecification({ ...EMPTY_VEHICLE_SPEC, ...(content.specification || {}) });
      setForm({
        ...emptyForm,
        carStyleId: vehicle?.carStyleId || series.find((item) => String(item.id) === String(content.carSeries?.id))?.styleId || "",
        carSeriesId: content.carSeries?.id || vehicle?.carSeriesId || "",
        name: content.name || "",
        salePercent: content.salePercent ?? "0",
        modelYear: content.modelYear || new Date().getFullYear(),
        imageUrl: content.imageUrl || vehicle?.imageUrl || "",
        videoUrl: content.videoUrl || "",
        visible: content.visible !== false,
      });
    } catch (error) {
      setMessage(error.message);
    }
  }, [emptyForm, series, vehicle, vehicleId]);

  useEffect(() => {
    setActiveTab("general");
    setPriceForm({ exteriorColorName: "", interiorColorName: "", price: "", exImageUrl: "", inImageUrl: "" });
    setArticleForm({ id: "", type: "FEATURE", title: "", content: "", imageUrl: "", published: true });
    setGalleryUrl("");
    if (isCreate) {
      setDetail(null);
      setForm(emptyForm);
      setSpecification(EMPTY_VEHICLE_SPEC);
      setMessage("");
      return;
    }
    loadDetail();
  }, [emptyForm, isCreate, loadDetail]);

  async function resolveCarSeriesId() {
    if (!isCreate) return form.carSeriesId;
    let carStyleId = form.carStyleId;
    if (isCreatingStyle) {
      const name = form.carStyleName.trim();
      const created = await createVehicleStyle({ name, description: `Kiểu dáng ${name}` });
      carStyleId = created?.id;
    }
    if (isCreatingSeries) {
      const name = form.carSeriesName.trim();
      const created = await createVehicleSeries({ carStyleId, name, description: `Dòng xe ${name}` });
      return created?.id;
    }
    return form.carSeriesId;
  }

  function normalizedSpec() {
    return {
      ...specification,
      numberOfSeats: numberOrNull(specification.numberOfSeats),
      length: numberOrNull(specification.length),
      width: numberOrNull(specification.width),
      height: numberOrNull(specification.height),
      capacity: numberOrNull(specification.capacity),
      cylinder: numberOrNull(specification.cylinder),
      maximumSpeed: numberOrNull(specification.maximumSpeed),
      grossWeightAllowance: numberOrNull(specification.grossWeightAllowance),
    };
  }

  async function saveGeneralAndSpecs(event) {
    event.preventDefault();
    setBusy(true);
    setMessage("");
    try {
      const carSeriesId = await resolveCarSeriesId();
      const payload = {
        carSeriesId,
        name: form.name.trim(),
        salePercent: Number(form.salePercent || 0),
        modelYear: Number(form.modelYear),
        imageUrl: form.imageUrl || "",
        videoUrl: form.videoUrl || "",
        visible: form.visible,
      };
      const savedVehicle = isCreate ? await createVehicle(payload) : await updateVehicle(vehicleId, payload);
      const savedVehicleId = savedVehicle?.id || vehicleId;
      await saveVehicleSpecification(savedVehicleId, normalizedSpec());
      if (isCreate && priceForm.price) {
        await saveVehiclePrice(savedVehicleId, {
          exteriorColorName: priceForm.exteriorColorName,
          interiorColorName: priceForm.interiorColorName,
          price: Number(priceForm.price),
          exImageUrl: priceForm.exImageUrl || form.imageUrl,
          inImageUrl: priceForm.inImageUrl,
        });
      }
      await onSaved(savedVehicleId, isCreate ? "Đã thêm xe." : "Đã lưu thông tin xe.");
      if (!isCreate) await loadDetail();
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }

  async function savePrice(event, existingPrice = null) {
    event.preventDefault();
    const source = existingPrice || priceForm;
    setBusy(true);
    setMessage("");
    try {
      await saveVehiclePrice(vehicleId, {
        exteriorColorId: source.exteriorColorId,
        exteriorColorName: source.exteriorColorName,
        interiorColorId: source.interiorColorId,
        interiorColorName: source.interiorColorName,
        price: Number(source.price),
        exImageUrl: source.exImageUrl,
        inImageUrl: source.inImageUrl,
      });
      setPriceForm({ exteriorColorName: "", interiorColorName: "", price: "", exImageUrl: "", inImageUrl: "" });
      setMessage("Đã lưu giá theo màu.");
      await loadDetail();
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }

  async function removePrice(price) {
    setBusy(true);
    setMessage("");
    try {
      await deleteVehiclePrice(vehicleId, price.exteriorColorId, price.interiorColorId);
      setMessage("Đã xóa cấu hình giá.");
      await loadDetail();
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }

  async function saveArticle(event) {
    event.preventDefault();
    setBusy(true);
    setMessage("");
    try {
      const payload = {
        type: articleForm.type,
        title: articleForm.title,
        content: articleForm.content,
        imageUrl: articleForm.imageUrl,
        carVersionId: vehicleId,
        published: articleForm.published,
      };
      if (articleForm.id) await updateArticle(articleForm.id, payload);
      else await createArticle(payload);
      setArticleForm({ id: "", type: "FEATURE", title: "", content: "", imageUrl: "", published: true });
      setMessage("Đã lưu bài viết của xe.");
      await loadDetail();
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }

  async function hideVehicleArticle(articleId) {
    setBusy(true);
    setMessage("");
    try {
      await hideArticle(articleId);
      setMessage("Đã ẩn bài viết.");
      await loadDetail();
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }

  async function addGallery(event) {
    event.preventDefault();
    if (!galleryUrl) return;
    setBusy(true);
    setMessage("");
    try {
      await addVehicleGallery(vehicleId, { imageUrl: galleryUrl });
      setGalleryUrl("");
      setMessage("Đã thêm ảnh gallery.");
      await loadDetail();
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }

  async function uploadGalleryFiles(event) {
    const files = Array.from(event.target.files || []);
    if (!files.length) return;
    setBusy(true);
    setMessage("");
    try {
      for (const file of files) {
        const result = await uploadMedia(file, "CAR_GALLERY");
        if (result?.secureUrl) await addVehicleGallery(vehicleId, { imageUrl: result.secureUrl });
      }
      setMessage("Đã tải ảnh gallery lên Cloudinary.");
      await loadDetail();
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
      event.target.value = "";
    }
  }

  async function replaceGallery(gallery, imageUrl) {
    setBusy(true);
    setMessage("");
    try {
      await updateVehicleGallery(vehicleId, gallery.id, { imageUrl });
      setMessage("Đã cập nhật ảnh gallery.");
      await loadDetail();
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }

  async function removeGallery(galleryId) {
    setBusy(true);
    setMessage("");
    try {
      await deleteVehicleGallery(vehicleId, galleryId);
      setMessage("Đã xóa ảnh gallery.");
      await loadDetail();
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }

  const isPage = variant === "page";

  return (
    <div className={isPage ? "manager-editor-page-shell" : "manager-modal-backdrop"} role={isPage ? undefined : "presentation"}>
      <section className={isPage ? "manager-editor-page" : "manager-modal"} role={isPage ? "region" : "dialog"} aria-modal={isPage ? undefined : "true"} aria-labelledby="manager-vehicle-dialog-title">
        <header className="manager-modal-head">
          <div>
            <p className="eyebrow">{isCreate ? "Thêm xe" : "Chi tiết xe"}</p>
            <h2 id="manager-vehicle-dialog-title">{isCreate ? "Thêm phiên bản xe" : form.name || vehicle?.name}</h2>
          </div>
          <button className="btn btn-ghost" type="button" onClick={onClose}>{isPage ? "Quay lại" : "Đóng"}</button>
        </header>
        <div className="manager-modal-body">
          <Feedback message={message} />
          {!isCreate && !detail ? <div className="status-box">Đang tải nội dung chi tiết...</div> : null}
          <nav className="role-tabs manager-editor-tabs" aria-label="Các nhóm thông tin xe">
            {VEHICLE_EDITOR_TABS.map(([value, label]) => (
              <button className={activeTab === value ? "active" : ""} key={value} type="button" onClick={() => setActiveTab(value)} disabled={isCreate && !["general", "specs", "prices"].includes(value)}>
                {label}
              </button>
            ))}
          </nav>
          {activeTab === "general" || activeTab === "specs" || (isCreate && activeTab === "prices") ? (
            <form className="ops-form" onSubmit={saveGeneralAndSpecs}>
              {activeTab === "general" ? (
                <VehicleGeneralForm
                  availableSeries={availableSeries}
                  form={form}
                  isCreate={isCreate}
                  isCreatingSeries={isCreatingSeries}
                  isCreatingStyle={isCreatingStyle}
                  setForm={setForm}
                  styles={styles}
                />
              ) : null}
              {activeTab === "specs" ? <VehicleSpecificationForm specification={specification} setSpecification={setSpecification} isCreate={isCreate} /> : null}
              {isCreate && activeTab === "prices" ? <VehiclePriceCreateForm priceForm={priceForm} setPriceForm={setPriceForm} /> : null}
              <div className="row-actions manager-modal-actions">
                <button className="btn btn-primary" disabled={busy} type="submit">{busy ? "Đang lưu..." : isCreate ? "Thêm xe" : "Lưu thay đổi"}</button>
                <button className="btn btn-ghost" type="button" onClick={onClose}>Hủy</button>
              </div>
            </form>
          ) : null}
          {!isCreate && activeTab === "prices" ? (
            <VehiclePricePanel detail={detail} busy={busy} priceForm={priceForm} setPriceForm={setPriceForm} onSave={savePrice} onRemove={removePrice} />
          ) : null}
          {!isCreate && activeTab === "articles" ? (
            <VehicleArticlePanel articleForm={articleForm} setArticleForm={setArticleForm} detail={detail} busy={busy} onSave={saveArticle} onHide={hideVehicleArticle} />
          ) : null}
          {!isCreate && activeTab === "gallery" ? (
            <VehicleGalleryPanel busy={busy} detail={detail} galleryUrl={galleryUrl} setGalleryUrl={setGalleryUrl} onAdd={addGallery} onUpload={uploadGalleryFiles} onReplace={replaceGallery} onRemove={removeGallery} />
          ) : null}
        </div>
      </section>
    </div>
  );
}

function VehicleGeneralForm({ availableSeries, form, isCreate, isCreatingSeries, isCreatingStyle, setForm, styles }) {
  return (
    <div className="manager-detail-block">
      <h3>Thông tin chung</h3>
      <div className="manager-editor-section">
        <h4>Thông tin cơ bản</h4>
        <div className="manager-editor-grid compact">
          <label className="manager-field">
            <span>Kiểu dáng</span>
            <select className="field" required value={form.carStyleId} onChange={(event) => {
              const nextStyleId = event.target.value;
              setForm({ ...form, carStyleId: nextStyleId, carSeriesId: nextStyleId === NEW_OPTION_VALUE ? NEW_OPTION_VALUE : "" });
            }}>
              <option value="">Chọn kiểu dáng</option>
              {styles.map((style) => <option key={style.id} value={style.id}>{style.name}</option>)}
              {isCreate ? <option value={NEW_OPTION_VALUE}>Nhập kiểu dáng mới</option> : null}
            </select>
          </label>
          {isCreatingStyle ? <TextField label="Tên kiểu dáng mới" value={form.carStyleName} onChange={(value) => setForm({ ...form, carStyleName: value })} required /> : null}
          <label className="manager-field">
            <span>Dòng xe</span>
            <select className="field" disabled={isCreatingStyle} required value={form.carSeriesId} onChange={(event) => setForm({ ...form, carSeriesId: event.target.value })}>
              <option value="">Chọn dòng xe</option>
              {availableSeries.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
              {isCreate ? <option value={NEW_OPTION_VALUE}>Nhập dòng xe mới</option> : null}
            </select>
          </label>
          {isCreatingSeries ? <TextField label="Tên dòng xe mới" value={form.carSeriesName} onChange={(value) => setForm({ ...form, carSeriesName: value })} required /> : null}
          <TextField label="Tên phiên bản" value={form.name} onChange={(value) => setForm({ ...form, name: value })} required />
          <TextField label="Năm mẫu xe" type="number" value={form.modelYear} onChange={(value) => setForm({ ...form, modelYear: value })} required />
          <TextField label="Giảm giá (%)" type="number" value={form.salePercent} onChange={(value) => setForm({ ...form, salePercent: value })} />
        </div>
      </div>
      <div className="manager-editor-section">
        <h4>Hình ảnh & video</h4>
        <div className="manager-media-layout">
          <MediaUploadField label="Ảnh đại diện" required={isCreate} value={form.imageUrl} onChange={(value) => setForm({ ...form, imageUrl: value })} context="CAR_GALLERY" />
          <MediaUploadField label="Video giới thiệu" value={form.videoUrl} onChange={(value) => setForm({ ...form, videoUrl: value })} context="CAR_VIDEO" accept="video/*" preview="video" placeholder="https://.../video-xe.mp4" />
        </div>
      </div>
      <label className="manager-checkbox"><input type="checkbox" checked={form.visible} onChange={(event) => setForm({ ...form, visible: event.target.checked })} /> Hiển thị trên website</label>
    </div>
  );
}

function VehicleSpecificationForm({ specification, setSpecification, isCreate }) {
  return (
    <div className="manager-detail-block">
      <h3>Thông số kỹ thuật</h3>
      <div className="manager-editor-grid">
        {SPEC_FIELDS.map(([key, label, type, required]) => (
          <TextField key={key} label={label} type={type} value={specification[key] || ""} required={isCreate ? required : false} onChange={(value) => setSpecification({ ...specification, [key]: value })} />
        ))}
      </div>
    </div>
  );
}

function VehiclePriceCreateForm({ priceForm, setPriceForm }) {
  return (
    <div className="manager-detail-block">
      <h3>Giá và màu đầu tiên</h3>
      <div className="manager-editor-grid">
        <TextField label="Màu ngoại thất" value={priceForm.exteriorColorName} onChange={(value) => setPriceForm({ ...priceForm, exteriorColorName: value })} required />
        <TextField label="Màu nội thất" value={priceForm.interiorColorName} onChange={(value) => setPriceForm({ ...priceForm, interiorColorName: value })} required />
        <TextField label="Giá bán" type="number" value={priceForm.price} onChange={(value) => setPriceForm({ ...priceForm, price: value })} required />
        <MediaUploadField label="Ảnh ngoại thất" value={priceForm.exImageUrl} onChange={(value) => setPriceForm({ ...priceForm, exImageUrl: value })} context="CAR_PRICE_EXTERIOR" />
        <MediaUploadField label="Ảnh nội thất" value={priceForm.inImageUrl} onChange={(value) => setPriceForm({ ...priceForm, inImageUrl: value })} context="CAR_PRICE_INTERIOR" />
      </div>
    </div>
  );
}

function VehiclePricePanel({ busy, detail, onRemove, onSave, priceForm, setPriceForm }) {
  const [editingPrices, setEditingPrices] = useState({});

  function setPriceDraft(key, next) {
    setEditingPrices((current) => ({ ...current, [key]: next }));
  }

  return (
    <div className="manager-detail-block">
      <h3>Giá & màu</h3>
      <form className="ops-form manager-subform" onSubmit={(event) => onSave(event)}>
        <VehiclePriceCreateForm priceForm={priceForm} setPriceForm={setPriceForm} />
        <button className="btn btn-secondary" disabled={busy} type="submit">Thêm tổ hợp màu</button>
      </form>
      <div className="manager-price-list">
        {(detail?.prices || []).map((price) => {
          const key = `${price.exteriorColorId}-${price.interiorColorId}`;
          const draft = editingPrices[key] || { ...price, price: toText(price.price) };
          return (
            <article key={key}>
              <strong>{price.exteriorColorName} / {price.interiorColorName}</strong>
              <div className="manager-editor-grid">
                <TextField label="Giá bán" type="number" value={draft.price} onChange={(value) => setPriceDraft(key, { ...draft, price: value })} />
                <MediaUploadField label="Ảnh ngoại thất" value={draft.exImageUrl || ""} onChange={(value) => setPriceDraft(key, { ...draft, exImageUrl: value })} context="CAR_PRICE_EXTERIOR" />
                <MediaUploadField label="Ảnh nội thất" value={draft.inImageUrl || ""} onChange={(value) => setPriceDraft(key, { ...draft, inImageUrl: value })} context="CAR_PRICE_INTERIOR" />
              </div>
              <div className="row-actions">
                <button className="btn btn-secondary" disabled={busy} type="button" onClick={(event) => onSave(event, draft)}>Lưu giá</button>
                <button className="btn btn-ghost" disabled={busy} type="button" onClick={() => onRemove(price)}>Xóa</button>
              </div>
            </article>
          );
        })}
        {!detail?.prices?.length ? <p className="muted-text">Chưa cấu hình tổ hợp màu/giá.</p> : null}
      </div>
    </div>
  );
}

function VehicleArticlePanel({ articleForm, busy, detail, onHide, onSave, setArticleForm }) {
  return (
    <div className="manager-detail-block">
      <h3>Bài viết của xe</h3>
      <form className="ops-form manager-subform" onSubmit={onSave}>
        <div className="manager-editor-grid">
          <TextField label="Loại bài viết" value={articleForm.type} onChange={(value) => setArticleForm({ ...articleForm, type: value })} required />
          <TextField label="Tiêu đề" value={articleForm.title} onChange={(value) => setArticleForm({ ...articleForm, title: value })} required />
          <MediaUploadField label="Ảnh bài viết" value={articleForm.imageUrl} onChange={(value) => setArticleForm({ ...articleForm, imageUrl: value })} context="ARTICLE_IMAGE" />
        </div>
        <label className="manager-field">
          <span>Nội dung</span>
          <textarea className="field manager-textarea" required value={articleForm.content} onChange={(event) => setArticleForm({ ...articleForm, content: event.target.value })} />
        </label>
        <label className="manager-checkbox"><input type="checkbox" checked={articleForm.published} onChange={(event) => setArticleForm({ ...articleForm, published: event.target.checked })} /> Xuất bản công khai</label>
        <div className="row-actions">
          <button className="btn btn-primary" disabled={busy} type="submit">{articleForm.id ? "Lưu bài viết" : "Thêm bài viết"}</button>
          {articleForm.id ? <button className="btn btn-ghost" type="button" onClick={() => setArticleForm({ id: "", type: "FEATURE", title: "", content: "", imageUrl: "", published: true })}>Tạo mới</button> : null}
        </div>
      </form>
      <div className="manager-table-list">
        {(detail?.articles || []).map((article) => (
          <article key={article.id}>
            <div><strong>{article.title}</strong><small>{article.type} · {article.published ? "Đã xuất bản" : "Đã ẩn"}</small></div>
            <div className="row-actions">
              <button className="btn btn-ghost" type="button" onClick={() => setArticleForm({ ...article, id: article.id })}>Sửa</button>
              {article.published ? <button className="btn btn-ghost" disabled={busy} type="button" onClick={() => onHide(article.id)}>Ẩn</button> : null}
            </div>
          </article>
        ))}
        {!detail?.articles?.length ? <p className="muted-text">Chưa có bài viết gắn với xe này.</p> : null}
      </div>
    </div>
  );
}

function VehicleGalleryPanel({ busy, detail, galleryUrl, onAdd, onRemove, onReplace, onUpload, setGalleryUrl }) {
  return (
    <div className="manager-detail-block">
      <h3>Gallery</h3>
      <div className="manager-gallery-upload-row">
        <label className="btn btn-secondary">
          Tải nhiều ảnh
          <input className="visually-hidden" type="file" accept="image/*" multiple onChange={onUpload} />
        </label>
      </div>
      <form className="manager-filter-row" onSubmit={onAdd}>
        <input className="field" value={galleryUrl} onChange={(event) => setGalleryUrl(event.target.value)} placeholder="Hoặc nhập URL hình ảnh" />
        <button className="btn btn-secondary" disabled={busy || !galleryUrl} type="submit">Thêm ảnh</button>
      </form>
      <div className="manager-gallery-grid">
        {(detail?.galleries || []).map((gallery) => (
          <article key={gallery.id}>
            <span style={{ backgroundImage: `url(${gallery.imageUrl})` }} />
            <MediaUploadField label="URL ảnh" value={gallery.imageUrl} onChange={(value) => onReplace(gallery, value)} context="CAR_GALLERY" showPreview={false} />
            <button className="btn btn-ghost" disabled={busy} type="button" onClick={() => onRemove(gallery.id)}>Xóa</button>
          </article>
        ))}
      </div>
    </div>
  );
}

function TextField({ label, value, onChange, type = "text", required = false }) {
  return (
    <label className="manager-field">
      <span>{label}</span>
      <input className="field" required={required} type={type} value={value} onChange={(event) => onChange(event.target.value)} />
    </label>
  );
}

function ArticlePanel() {
  const [items, setItems] = useState([]);
  const [state, setState] = useState({ loading: true, error: "", message: "" });

  const load = useCallback(async () => {
    try {
      const articles = await getManagerArticles();
      setItems(unwrapList(articles));
      setState((value) => ({ ...value, loading: false, error: "" }));
    } catch (error) {
      setState((value) => ({ ...value, loading: false, error: error.message }));
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  async function hide(id) {
    try {
      await hideArticle(id);
      setState((value) => ({ ...value, message: "Đã ẩn bài viết." }));
      await load();
    } catch (error) {
      setState((value) => ({ ...value, message: error.message }));
    }
  }

  return (
    <div className="manager-content-grid manager-vehicle-layout list-only">
      <section className="ops-panel">
        <div className="ops-panel-head">
          <div><p className="eyebrow">Tin tức</p><h2>Danh sách bài viết</h2></div>
          <Link className="btn btn-primary" href="/dashboard/manager/articles/new">Thêm bài viết</Link>
        </div>
        <Feedback message={state.message} />
        <PanelState loading={state.loading} error={state.error} empty={!items.length}>
          <div className="manager-table-list">
            {items.map((item) => (
              <article key={item.id}>
                <div><strong>{item.title}</strong><small>{item.type} · {item.published ? "Đã xuất bản" : "Đã ẩn"}</small></div>
                <div className="row-actions">
                  <Link className="btn btn-ghost" href={`/dashboard/manager/articles/${item.id}`}>Sửa</Link>
                  {item.published ? <button className="btn btn-ghost" type="button" onClick={() => hide(item.id)}>Ẩn</button> : null}
                </div>
              </article>
            ))}
          </div>
        </PanelState>
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
    } catch (error) {
      setState((value) => ({ ...value, loading: false, error: error.message }));
    }
  }
  useEffect(() => { load(); }, []);

  async function submit(event) {
    event.preventDefault();
    setState((value) => ({ ...value, busy: true, message: "" }));
    try {
      const payload = { ...form, latitude: Number(form.latitude), longitude: Number(form.longitude) };
      if (editing) await updateDealership(editing, payload);
      else await createDealership(payload);
      setEditing("");
      setForm(empty);
      setState((value) => ({ ...value, busy: false, message: "Đã lưu thông tin đại lý." }));
      await load();
    } catch (error) {
      setState((value) => ({ ...value, busy: false, message: error.message }));
    }
  }

  async function deactivate(id) {
    try {
      await deactivateDealership(id);
      setState((value) => ({ ...value, message: "Đại lý đã ngừng hiển thị công khai." }));
      await load();
    } catch (error) {
      setState((value) => ({ ...value, message: error.message }));
    }
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
    } catch (error) {
      setState((value) => ({ ...value, loading: false, error: error.message }));
    }
  }
  useEffect(() => { load(); }, []);

  async function submit(event) {
    event.preventDefault();
    setState((value) => ({ ...value, busy: true, message: "" }));
    try {
      const payload = { ...form, price: Number(form.price) };
      if (editing) await updateAccessory(editing, payload);
      else await createAccessory(payload);
      setEditing("");
      setForm(empty);
      setState((value) => ({ ...value, busy: false, message: "Đã lưu phụ kiện." }));
      await load();
    } catch (error) {
      setState((value) => ({ ...value, busy: false, message: error.message }));
    }
  }

  async function hide(id) {
    try {
      await hideAccessory(id);
      setState((value) => ({ ...value, message: "Đã ẩn phụ kiện khỏi website." }));
      await load();
    } catch (error) {
      setState((value) => ({ ...value, message: error.message }));
    }
  }

  async function updateLink(action) {
    if (!editing || !linkedVehicleId) return;
    try {
      if (action === "attach") await attachAccessoryToVehicle(editing, linkedVehicleId);
      else await detachAccessoryFromVehicle(editing, linkedVehicleId);
      setState((value) => ({ ...value, message: action === "attach" ? "Đã gắn phụ kiện cho xe." : "Đã gỡ phụ kiện khỏi xe." }));
    } catch (error) {
      setState((value) => ({ ...value, message: error.message }));
    }
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
        </form>
      </section>
    </div>
  );
}

function getRoleChartItems(stats) {
  const total = Number(stats?.total || 0);
  let cursor = 0;
  return LOWER_ROLES.map((role) => {
    const count = Number(stats?.byRole?.[role] || 0);
    const percent = total > 0 ? (count / total) * 100 : 0;
    const start = cursor;
    const end = cursor + percent;
    cursor = end;
    return {
      role,
      count,
      percent,
      color: LOWER_ROLE_CHART_COLORS[role],
      label: roleLabel(role),
      segment: total > 0 ? `${LOWER_ROLE_CHART_COLORS[role]} ${start}% ${end}%` : "",
    };
  });
}

function getRoleChartBackground(items) {
  const segments = items.map((item) => item.segment).filter(Boolean);
  return segments.length ? `conic-gradient(${segments.join(", ")})` : "conic-gradient(#e5e7eb 0 100%)";
}

function UserPanel() {
  const [items, setItems] = useState([]);
  const [stats, setStats] = useState(null);
  const [role, setRole] = useState("");
  const [keyword, setKeyword] = useState("");
  const [profile, setProfile] = useState(null);
  const [state, setState] = useState({ loading: true, error: "", message: "", busy: false });

  const load = useCallback(async () => {
    try {
      const [users, nextStats] = await Promise.all([getManagerUsers({ keyword, role, page: 0, size: 50 }), getManagerUserStats()]);
      setItems(unwrapList(users));
      setStats(nextStats);
      setState((value) => ({ ...value, loading: false, error: "" }));
    } catch (error) {
      setState((value) => ({ ...value, loading: false, error: error.message }));
    }
  }, [keyword, role]);

  useEffect(() => {
    load();
  }, [load]);

  async function selectUser(user) {
    try {
      const result = await getManagerUserProfile(user.id);
      setProfile(result);
    } catch (error) {
      setState((value) => ({ ...value, message: error.message }));
    }
  }

  async function submitProfile(event) {
    event.preventDefault();
    setState((value) => ({ ...value, busy: true, message: "" }));
    try {
      await updateManagerUserProfile(profile);
      setState((value) => ({ ...value, busy: false, message: "Đã cập nhật hồ sơ người dùng." }));
      await load();
    } catch (error) {
      setState((value) => ({ ...value, busy: false, message: error.message }));
    }
  }

  const roleChartItems = getRoleChartItems(stats);
  const chartTotal = Number(stats?.total || 0);
  const activeTotal = Number(stats?.byStatus?.ACTIVE || 0);
  const activePercent = chartTotal > 0 ? Math.round((activeTotal / chartTotal) * 100) : 0;

  return (
    <div className="manager-users">
      <section className="manager-role-chart-card">
        <div className="manager-role-chart-copy">
          <p className="eyebrow">Nhân sự</p>
          <h2>Phân bổ người dùng cấp dưới</h2>
          <div className="manager-role-chart-meta">
            <span>Tổng người dùng <strong>{chartTotal}</strong></span>
            <span>Đang hoạt động <strong>{activeTotal}</strong> <small>{activePercent}%</small></span>
          </div>
        </div>
        <div className="manager-role-chart-visual" style={{ "--role-chart": getRoleChartBackground(roleChartItems) }}>
          <div className="manager-role-donut" aria-label={`Tổng người dùng cấp dưới ${chartTotal}`}>
            <span>Tổng</span>
            <strong>{chartTotal}</strong>
          </div>
        </div>
        <div className="manager-role-legend" aria-label="Chú thích vai trò">
          {roleChartItems.map((item) => (
            <div key={item.role}>
              <i style={{ background: item.color }} />
              <span>{item.label}</span>
              <strong>{item.count}</strong>
              <small>{Math.round(item.percent)}%</small>
            </div>
          ))}
        </div>
      </section>
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
              <MediaUploadField label="Ảnh đại diện" value={profile.avatarUrl || ""} onChange={(value) => setProfile({ ...profile, avatarUrl: value })} context="USER_AVATAR" />
              <button className="btn btn-primary" disabled={state.busy} type="submit">{state.busy ? "Đang lưu..." : "Lưu hồ sơ"}</button>
            </form>
          )}
        </section>
      </div>
    </div>
  );
}


