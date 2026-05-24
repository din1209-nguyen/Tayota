import Link from "next/link";

const tools = [
  ["So sánh riêng", "Đặt các phiên bản cân nhắc cạnh nhau để ra quyết định rõ ràng hơn.", "/compare"],
  ["Lái thử cá nhân hóa", "Chọn ngày, khung giờ và hình thức trải nghiệm phù hợp lịch của bạn.", "/appointments/test-drive"],
  ["Dịch vụ sở hữu", "Đặt lịch bảo dưỡng và chăm sóc xe với thông tin minh bạch.", "/appointments/service"],
  ["AI tư vấn", "Hỏi nhanh về dòng xe, thông số và gợi ý lựa chọn theo nhu cầu.", "#ai-chat"],
];

export default function ConciergeTools() {
  return (
    <section className="section service-band">
      <div className="shell-container">
        <p className="eyebrow">Dịch vụ cá nhân hóa</p>
        <h2>Mỗi thao tác đều gắn với một nhu cầu thật</h2>
        <div className="tool-grid">
          {tools.map(([title, text, href]) => (
            <Link className="tool-card" href={href} key={title}>
              <h3>{title}</h3>
              <p>{text}</p>
              <span>Mở</span>
            </Link>
          ))}
        </div>
      </div>
    </section>
  );
}
