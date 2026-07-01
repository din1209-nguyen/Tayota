"use client";

const URL_PATTERN = /(https?:\/\/[^\s<>"']+)/g;

export default function LinkifiedText({ text }) {
  const content = String(text || "");
  const parts = content.split(URL_PATTERN);

  return parts.map((part, index) => {
    if (!part.startsWith("http://") && !part.startsWith("https://")) return part;
    return (
      <a className="inline-notification-link" href={part} key={`${part}-${index}`}>
        {part}
      </a>
    );
  });
}
