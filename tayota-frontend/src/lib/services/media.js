import { apiFetch } from "@/lib/api";

export function uploadMedia(file, context) {
  const body = new FormData();
  body.append("file", file);
  body.append("context", context);
  return apiFetch("/operation/media/uploads", { method: "POST", body });
}
