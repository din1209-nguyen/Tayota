import Link from "next/link";
import { apiFetch } from "@/lib/api";
import {
  formatVnd,
  getVehicleHighlights,
  getVehicleImage,
  getVehicleName,
  getVehiclePrice,
  getVehicleSeriesName,
  getVehicleStyleName,
} from "@/lib/format";
import SpecificationTable from "@/components/vehicles/SpecificationTable";
import VehicleImageButton from "@/components/vehicles/VehicleImageButton";

async function getDetail(id) {
  try {
    const vehicle = await apiFetch(`/car/catalog/car-versions/${id}`, { cache: "no-store" });
    return { vehicle, error: null };
  } catch (error) {
    return { vehicle: null, error: error.message };
  }
}

export default async function VehicleDetailPage({ params }) {
  const { id } = await params;
  const { vehicle, error } = await getDetail(id);

  if (error) {
    return (
      <section className="section">
        <div className="shell-container status-box">{error}</div>
      </section>
    );
  }

  const imageUrl = getVehicleImage(vehicle);
  const vehicleName = getVehicleName(vehicle);
  const prices = Array.isArray(vehicle?.prices) ? vehicle.prices : [];
  const galleries = Array.isArray(vehicle?.galleries) ? vehicle.galleries.filter((item) => item?.imageUrl) : [];
  const articles = Array.isArray(vehicle?.articles) ? vehicle.articles : [];
  const accessories = Array.isArray(vehicle?.accessories) ? vehicle.accessories : [];

  return (
    <section className="detail-page">
      <div
        className="detail-hero"
        style={
          imageUrl
            ? {
                backgroundImage: `linear-gradient(90deg, rgba(0, 0, 0, 0.9), rgba(5, 5, 5, 0.46) 46%, rgba(5, 5, 5, 0.18)), linear-gradient(180deg, transparent 72%, #ffffff 100%), url(${imageUrl})`,
              }
            : undefined
        }
      >
        <div className="shell-container detail-hero-inner">
          <div>
            <p className="eyebrow">{getVehicleStyleName(vehicle)}</p>
            <h1>{vehicleName}</h1>
            <p>{vehicle?.carSeries?.description || "Thiết kế tinh gọn, vận hành êm và sẵn sàng cho mọi hành trình riêng."}</p>
            <div className="hero-actions">
              <Link className="btn btn-primary" href={`/appointments/test-drive?carVersionId=${id}`}>
                Đăng ký lái thử
              </Link>
              <Link className="btn btn-secondary hero-dark" href="/dealerships">
                Liên hệ đại lý
              </Link>
              <Link className="btn btn-secondary hero-dark" href={`/compare?ids=${id}`}>
                So sánh xe
              </Link>
            </div>
          </div>
          <div className="detail-price">
            <span>Giá từ</span>
            <strong>{formatVnd(getVehiclePrice(vehicle))}</strong>
            <small>
              {getVehicleSeriesName(vehicle)} · {vehicle?.modelYear || "Năm mới nhất"}
            </small>
          </div>
        </div>
      </div>

      <div className="shell-container detail-overview">
        {getVehicleHighlights(vehicle).map((item) => (
          <div key={item.label}>
            <span>{item.label}</span>
            <strong>{item.value}</strong>
          </div>
        ))}
      </div>

      <div className="shell-container detail-content detail-spec-section" id="specifications">
        <div className="detail-spec-heading">
          <p className="eyebrow">Thông số</p>
          <h2>Thông tin xe liên quan</h2>
          <p className="muted-text">Các chỉ số chính để đọc nhanh và so sánh phiên bản phù hợp.</p>
        </div>
        <SpecificationTable specification={vehicle?.specification} />
      </div>

      {prices.length ? (
        <section className="shell-container detail-section price-detail-section">
          <div className="section-heading compact">
            <div>
              <p className="eyebrow">Màu sắc và giá</p>
              <h2>Các lựa chọn hiện có</h2>
            </div>
          </div>
          <div className="price-grid">
            {prices.map((price, index) => (
              <article className="price-card card" key={`${price.exteriorColorId}-${price.interiorColorId}-${index}`}>
                <div className="price-image-pair">
                  <div>
                    <span>Ngoại thất</span>
                    <VehicleImageButton className="price-image" src={price.exImageUrl} title={`${vehicleName} - ${price.exteriorColorName || "Ngoại thất"}`} />
                  </div>
                  <div>
                    <span>Nội thất</span>
                    <VehicleImageButton className="price-image" src={price.inImageUrl} title={`${vehicleName} - ${price.interiorColorName || "Nội thất"}`} />
                  </div>
                </div>
                <h3>
                  {price.exteriorColorName || "Ngoại thất"} / {price.interiorColorName || "Nội thất"}
                </h3>
                <strong>{formatVnd(price.price)}</strong>
              </article>
            ))}
          </div>
        </section>
      ) : null}

      {galleries.length ? (
        <section className="shell-container detail-section">
          <div className="section-heading compact">
            <div>
              <p className="eyebrow">Gallery</p>
              <h2>Góc nhìn chi tiết của {vehicleName}</h2>
            </div>
          </div>
          <div className="gallery-grid">
            {galleries.map((gallery, index) => (
              <figure className="gallery-card" key={gallery.id || gallery.imageUrl}>
                <VehicleImageButton className="gallery-tile" src={gallery.imageUrl} title={`${vehicleName} - ảnh ${index + 1}`} />
                <figcaption>{index === 0 ? "Ngoại thất" : index === 1 ? "Khoang lái" : "Trải nghiệm vận hành"}</figcaption>
              </figure>
            ))}
          </div>
        </section>
      ) : null}

      {articles.length ? (
        <section className="shell-container detail-section">
          <div className="section-heading compact">
            <div>
              <p className="eyebrow">Khám phá</p>
              <h2>Bài viết và điểm nổi bật theo từng phiên bản</h2>
            </div>
          </div>
          <div className="article-grid">
            {articles.map((article) => (
              <article className="article-card card" key={article.id}>
                {article.imageUrl ? <VehicleImageButton className="article-image" src={article.imageUrl} title={article.title} /> : null}
                <div>
                  <p className="eyebrow">{article.type || "Tính năng"}</p>
                  <h3>{article.title}</h3>
                  <p>{article.content}</p>
                </div>
              </article>
            ))}
          </div>
        </section>
      ) : null}

      {accessories.length ? (
        <section className="shell-container detail-section">
          <div className="section-heading compact">
            <div>
              <p className="eyebrow">Phụ kiện</p>
              <h2>Phụ kiện tương thích</h2>
            </div>
          </div>
          <div className="accessory-grid">
            {accessories.map((accessory) => (
              <article className="accessory-card card" key={accessory.id}>
                <h3>{accessory.model}</h3>
                <p>{accessory.description}</p>
                <strong>{formatVnd(accessory.price)}</strong>
              </article>
            ))}
          </div>
        </section>
      ) : null}

      <div className="mobile-sticky-cta">
        <Link className="btn btn-primary" href={`/appointments/test-drive?carVersionId=${id}`}>
          Đặt lái thử
        </Link>
        <Link className="btn btn-ghost" href="/dealerships">
          Đại lý
        </Link>
      </div>
    </section>
  );
}
