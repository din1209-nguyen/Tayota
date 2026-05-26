"use client";

import { useRef, useState } from "react";
import { uploadMedia } from "@/lib/services/media";

export default function MediaUploadField({
  label,
  value,
  onChange,
  context,
  accept = "image/*",
  placeholder = "https://...",
  required = false,
  preview = "image",
  showPreview = true,
}) {
  const inputRef = useRef(null);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");

  async function chooseFile(event) {
    const file = event.target.files?.[0];
    if (!file) return;
    setBusy(true);
    setMessage("");
    try {
      const result = await uploadMedia(file, context);
      onChange(result?.secureUrl || "");
      setMessage("Đã tải lên Cloudinary.");
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
      if (inputRef.current) inputRef.current.value = "";
    }
  }

  return (
    <div className="manager-field media-upload-field">
      <span>{label}</span>
      {showPreview && value ? (
        preview === "video" ? (
          <video className="media-upload-preview video" src={value} controls muted />
        ) : (
          <span className="media-upload-preview image" style={{ backgroundImage: `url(${value})` }} />
        )
      ) : null}
      <div className="media-upload-row">
        <input
          className="field"
          required={required}
          value={value || ""}
          onChange={(event) => onChange(event.target.value)}
          placeholder={placeholder}
          maxLength={1024}
        />
        <button className="btn btn-secondary" disabled={busy} type="button" onClick={() => inputRef.current?.click()}>
          {busy ? "Đang tải..." : "Tải lên"}
        </button>
      </div>
      <input ref={inputRef} className="visually-hidden" type="file" accept={accept} onChange={chooseFile} />
      {message ? <small className="media-upload-message">{message}</small> : null}
    </div>
  );
}
