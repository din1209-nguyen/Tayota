import { statusLabel } from "@/lib/format";

function serviceItemTypeLabel(type) {
  if (type === "PART") return "Phụ tùng";
  if (type === "LABOR") return "Công thợ";
  return statusLabel(type);
}

export default function ServiceBreakdown({
  items = [],
  formatCurrency,
  idPrefix = "service",
  emptyPartText = "Chưa có phụ tùng được cập nhật.",
  emptyLaborText = "Chưa có công thợ được cập nhật.",
}) {
  const partItems = items.filter((item) => item.itemType === "PART");
  const laborItems = items.filter((item) => item.itemType === "LABOR");
  const money = typeof formatCurrency === "function" ? formatCurrency : (value) => value;

  return (
    <div className="service-breakdown">
      <section className="service-breakdown-group" aria-labelledby={`${idPrefix}-parts-title`}>
        <header className="service-breakdown-head">
          <div>
            <h3 id={`${idPrefix}-parts-title`}>Phụ tùng đã thay</h3>
          </div>
          <span>{partItems.length} hạng mục</span>
        </header>
        <div className="service-item-list">
          {partItems.map((item) => (
            <article className="service-item-card service-part-card" key={item.id || item.itemName}>
              <div className="service-item-copy">
                <span>{serviceItemTypeLabel(item.itemType)}</span>
                <strong>{item.itemName || "Phụ tùng"}</strong>
                {item.note ? <small>{item.note}</small> : null}
              </div>
              <div className="service-item-meta service-item-meta-part">
                <div className="service-meta-row">
                  <div className="service-meta-field service-meta-quantity"><span>Số lượng</span><strong>{item.quantity || 1}</strong></div>
                  <div className="service-meta-field service-meta-money-field"><span>Đơn giá</span><strong>{money(item.unitPrice || 0)}</strong></div>
                </div>
                <div className="service-meta-row">
                  <div className="service-meta-field service-meta-category"><span>Hạng mục</span><strong>{statusLabel(item.billingType)}</strong></div>
                  <div className="service-meta-field service-meta-money-field service-meta-total"><span>Thành tiền</span><strong>{money(item.finalPrice || 0)}</strong></div>
                </div>
              </div>
            </article>
          ))}
          {!partItems.length ? <div className="status-box">{emptyPartText}</div> : null}
        </div>
      </section>

      <section className="service-breakdown-group" aria-labelledby={`${idPrefix}-labor-title`}>
        <header className="service-breakdown-head">
          <div>
            <h3 id={`${idPrefix}-labor-title`}>Công thực hiện</h3>
          </div>
          <span>{laborItems.length} hạng mục</span>
        </header>
        <div className="service-item-list">
          {laborItems.map((item) => (
            <article className="service-item-card service-labor-card" key={item.id || item.itemName}>
              <div className="service-item-copy">
                <span>{serviceItemTypeLabel(item.itemType)}</span>
                <strong>{item.itemName || "Công thợ"}</strong>
                {item.note ? <small>{item.note}</small> : null}
              </div>
              <div className="service-item-meta service-item-meta-labor">
                <div className="service-meta-row">
                  <div className="service-meta-field service-meta-category"><span>Hạng mục</span><strong>{statusLabel(item.billingType)}</strong></div>
                  <div className="service-meta-field service-meta-money-field service-meta-total"><span>Thành tiền</span><strong>{money(item.finalPrice || 0)}</strong></div>
                </div>
              </div>
            </article>
          ))}
          {!laborItems.length ? <div className="status-box">{emptyLaborText}</div> : null}
        </div>
      </section>
    </div>
  );
}
