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

// Lấy chi tiết xe ở Server Component để trả HTML có dữ liệu ngay từ server.
async function getDetail(id) {
  try {
    // Gọi API catalog chi tiết xe từ server và không lưu cache ở tầng Next.js.
    const vehicle = await apiFetch(`/car/catalog/car-versions/${id}`, { cache: "no-store" });

    // Trả dữ liệu xe khi API phản hồi thành công.
    return { vehicle, error: null };
  } catch (error) {
    // Trả lỗi thân thiện để trang vẫn render được trạng thái thất bại.
    return { vehicle: null, error: error.message };
  }
}

// Hiển thị trang giới thiệu xe bằng Server Component.
export default async function VehicleDetailPage({ params }) {
  // Lấy id xe từ dynamic route của Next.js.
  const { id } = await params;

  // Lấy dữ liệu chi tiết trước khi render HTML.
  const { vehicle, error } = await getDetail(id);

  // Hiển thị lỗi nếu backend không trả được dữ liệu xe.
  if (error) {
    return (
      <section className="section">
        <div className="shell-container status-box">{error}</div>
      </section>
    );
  }

  // Chuẩn hóa ảnh hero để dùng làm background nếu có.
  const imageUrl = getVehicleImage(vehicle);

  // Chuẩn hóa danh sách giá để tránh lỗi khi API trả thiếu dữ liệu.
  const prices = Array.isArray(vehicle?.prices) ? vehicle.prices : [];

  // Chuẩn hóa danh sách bài viết giới thiệu để render có điều kiện.
  const articles = Array.isArray(vehicle?.articles) ? vehicle.articles : [];

  // Chuẩn hóa danh sách phụ kiện để render có điều kiện.
  const accessories = Array.isArray(vehicle?.accessories) ? vehicle.accessories : [];

  // Trả về toàn bộ nội dung giới thiệu xe đã có dữ liệu server-side.
  return (
    <section className="detail-page">
      <div className="detail-hero" style={imageUrl ? { backgroundImage: `linear-gradient(90deg, rgba(5, 5, 5, 0.94), rgba(5, 5, 5, 0.28)), linear-gradient(180deg, transparent 72%, #ffffff 100%), url(${imageUrl})` } : undefined}>
        <div className="shell-container detail-hero-inner">
          <div>
            <p className="eyebrow">{getVehicleStyleName(vehicle)}</p>
            <h1>{getVehicleName(vehicle)}</h1>
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
            <small>{getVehicleSeriesName(vehicle)} · {vehicle?.modelYear || "Năm mới nhất"}</small>
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

      <div className="shell-container detail-content" id="specifications">
        <div>
          <p className="eyebrow">Thông số</p>
          <h2>Thông tin xe liên quan</h2>
          <p className="muted-text">Các thông tin được chuẩn hóa nhãn tiếng Việt từ dữ liệu catalogue.</p>
        </div>
        <SpecificationTable specification={vehicle?.specification} />
      </div>

      {prices.length ? (
        <section className="shell-container detail-section">
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
                    <div className="price-image" style={price.exImageUrl ? { backgroundImage: `url(${price.exImageUrl})` } : undefined} />
                  </div>
                  <div>
                    <span>Nội thất</span>
                    <div className="price-image" style={price.inImageUrl ? { backgroundImage: `url(${price.inImageUrl})` } : undefined} />
                  </div>
                </div>
                <h3>{price.exteriorColorName || "Ngoại thất"} / {price.interiorColorName || "Nội thất"}</h3>
                <strong>{formatVnd(price.price)}</strong>
              </article>
            ))}
          </div>
        </section>
      ) : null}

      {articles.length ? (
        <section className="shell-container detail-section">
          <div className="section-heading compact">
            <div>
              <p className="eyebrow">Khám phá</p>
              <h2>Ngoại thất, nội thất và an toàn</h2>
            </div>
          </div>
          <div className="article-grid">
            {articles.map((article) => (
              <article className="article-card card" key={article.id}>
                {article.imageUrl ? <div className="article-image" style={{ backgroundImage: `url(${article.imageUrl})` }} /> : null}
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
