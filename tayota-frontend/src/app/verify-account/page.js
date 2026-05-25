import { Suspense } from "react";
import VerifyAccountClient from "@/components/VerifyAccountClient";

export const metadata = {
  title: "Xác thực tài khoản | Tayota",
};

export default function VerifyAccountPage() {
  return (
    <main className="page-section auth-page">
      <Suspense fallback={<div className="form-panel">Đang mở liên kết xác thực...</div>}>
        <VerifyAccountClient />
      </Suspense>
    </main>
  );
}
