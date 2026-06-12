import LiveChatPanel from "@/components/chat/LiveChatPanel";

export const metadata = {
  title: "Trò chuyện trực tuyến | Tayota",
};

export default function LiveChatPage() {
  return (
    <section className="section ops-page">
      <div className="shell-container page-title">
        <p className="eyebrow">Support</p>
        <h1>Trò chuyện trực tuyến</h1>
      </div>
      <div className="shell-container">
        <LiveChatPanel />
      </div>
    </section>
  );
}
