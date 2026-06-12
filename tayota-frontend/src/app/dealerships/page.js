import DealerGrid from "@/components/dealerships/DealerGrid";
import { apiFetch } from "@/lib/api";
import { unwrapList } from "@/lib/format";

async function getDealerships() {
  try {
    const data = await apiFetch("/car/dealerships", { cache: "no-store" });
    return { dealerships: unwrapList(data), error: null };
  } catch (error) {
    return { dealerships: [], error: error.message };
  }
}

export const metadata = {
  title: "Đại lý | Tayota",
};

export default async function DealershipsPage() {
  const { dealerships, error } = await getDealerships();

  return (
    <section className="section dealerships-page">
      <div className="shell-container page-title">
        <p className="eyebrow">Hệ thống đại lý</p>
        <h1>Xem các đại lý Tayota</h1>
        <p>Chọn đại lý gần bạn để lái thử, bảo dưỡng hoặc nhận tư vấn trực tiếp.</p>
      </div>
      <div className="dealer-shell">
        {error ? <div className="shell-container status-box">{error}</div> : <DealerGrid dealerships={dealerships} />}
      </div>
    </section>
  );
}
