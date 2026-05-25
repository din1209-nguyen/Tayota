import Link from "next/link";
import { unwrapList } from "@/lib/format";
import { getAllCarVersions, getCarStylesWithVersions } from "@/lib/services/car";
import FeaturedVehicleBrowser from "@/components/home/FeaturedVehicleBrowser";

async function getVehicles() {
  try {
    const [vehicleData, styleData] = await Promise.all([
      getAllCarVersions(),
      getCarStylesWithVersions(),
    ]);
    return { vehicles: vehicleData, styles: unwrapList(styleData), error: null };
  } catch (error) {
    return { vehicles: [], styles: [], error: error.message };
  }
}

export default async function FeaturedVehicles() {
  const { vehicles, styles, error } = await getVehicles();

  return (
    <section className="section">
      <div className="shell-container section-heading">
        <div>
          <p className="eyebrow">Bộ sưu tập Tayota</p>
          <h2>Những phiên bản đang được quan tâm</h2>
        </div>
        <Link className="btn btn-ghost" href="/vehicles">
          Xem tất cả
        </Link>
      </div>
      {error ? <div className="shell-container status-box">Chưa kết nối được gateway catalog. Vui lòng kiểm tra NEXT_PUBLIC_API_BASE_URL hoặc khởi động backend.</div> : null}
      {!error && vehicles.length === 0 ? <div className="shell-container status-box">Hiện chưa có xe phù hợp trong catalog.</div> : null}
      {!error && vehicles.length ? <FeaturedVehicleBrowser vehicles={vehicles} styles={styles} /> : null}
    </section>
  );
}
