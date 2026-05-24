import LiveChatPanel from "@/components/chat/LiveChatPanel";

export const metadata = {
  title: "Live Chat | TAYOTA",
};

export default function LiveChatPage() {
  return (
    <section className="section ops-page">
      <div className="shell-container page-title">
        <p className="eyebrow">Support</p>
        <h1>Live chat</h1>
      </div>
      <div className="shell-container">
        <LiveChatPanel />
      </div>
    </section>
  );
}
