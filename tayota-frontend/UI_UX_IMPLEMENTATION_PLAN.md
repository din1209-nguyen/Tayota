# Tayota Frontend và UI/UX - Hướng dẫn cho Agent

> Đọc `../AGENTS.md` trước khi thay đổi frontend. File này mô tả frontend đang
> tồn tại và quy tắc UI/API khi triển khai tiếp. Backend luôn được gọi qua API
> Gateway; quy trình backend thuộc Docker Compose.

## 1. Stack và cấu trúc hiện tại

Frontend dùng Next.js App Router và React:

```text
src/
  app/                  # Route, layout và globals.css
  components/           # UI theo trang/domain
  lib/
    api.js              # Axios instance chung, bearer token và single-flight refresh
    session.js          # Access token, current user và dashboard mapping
    format.js
    services/           # API wrapper theo domain
```

`package.json` hiện có Next.js, React, Axios và STOMP client cho realtime chat.

## 2. Route giao diện đã có

### Trang công khai

| Route | Nội dung |
| --- | --- |
| `/` | Trang chủ ghép các section giới thiệu. |
| `/vehicles`, `/vehicles/[id]` | Catalog và chi tiết xe. |
| `/compare` | So sánh xe. |
| `/dealerships` | Đại lý. |
| `/appointments/test-drive` | Đặt lịch lái thử. |
| `/appointments/service` | Đặt lịch dịch vụ. |
| `/auth/login`, `/auth/register`, `/verify-account` | Xác thực người dùng. |
| `/reviews/[token]` | Gửi đánh giá qua token. |
| `/support/live-chat` | Live chat hỗ trợ. |

### Workspace

| Route | Role/giao diện hiện có |
| --- | --- |
| `/dashboard` | Điểm điều hướng workspace. |
| `/dashboard/admin` | Admin. |
| `/dashboard/admin/users/[userId]` | Chi tiết tài khoản, nhà cung cấp đăng nhập, hồ sơ, đại lý nhân sự, bảo mật và phiên đăng nhập do admin quản lý. |
| `/dashboard/manager` | Manager xử lý live chat và quản trị nội dung website. |
| `/dashboard/advisor` | Service advisor. |
| `/dashboard/assistant` | Assistant. |
| `/dashboard/mechanic` | Mechanic. |
| `/dashboard/user` | Người dùng mặc định. |

Backend có role `MANAGER`; frontend map role này vào `/dashboard/manager`.
Workspace Manager gồm các tab Live Chat, Xe, Bài viết, Đại lý, Phụ kiện và
Người dùng; không bao gồm báo cáo doanh thu hoặc thao tác bảo mật tài khoản.

## 3. Service API theo domain

Component/page nên gọi service wrapper thay vì tự rải request API.

| Service trong `src/lib/services` | API gateway dùng hiện tại |
| --- | --- |
| `auth.js` | `/user/login`, `/user/register`, `/user/verify-account`, `/user/logout`, `/user/me`. |
| `car.js` | `/car/catalog/*`, `/car/dealerships`, `/car/accessories`. |
| `appointments.js` | `/operation/appointments/*`. |
| `chat.js` | `/user/chat/*`, `/user/assistant/chat/*`, `/ai/api/v1/chat`, `/user/chat/ws`. |
| `notifications.js` | `/operation/notifications/*`. |
| `reviews.js` | `/operation/reviews/*`. |
| `workorders.js` | `/operation/workorders/mechanic/*`. |
| `admin.js` | `/user/create-account`, `/user/admin/users*` (gồm đổi đại lý nhân sự), `/user/profile*`, `/user/devices/*`, `/user/revoke/*`, `/user/ban/*`, `/user/unban/*`, `/ai/api/v1/documents*`. |

Màn quản trị tài khoản hiển thị `loginProvider` trong danh sách và chi tiết.
Admin chỉ thay đổi đại lý cho `SERVICE_ADVISOR` và `MECHANIC` thuộc phạm vi
quản lý của mình. Tab tài liệu AI phải thể hiện rõ file được chọn, trạng thái
tải lên/lập chỉ mục (`queued`, `running`, `success`, `failed`) và kết quả danh
sách tài liệu đã lưu.

`StaffChatWorkspace` là UI dùng chung cho workspace Assistant và Manager; nó
dùng `chat.js` để tải phiên `WAITING`/`CHATTING`, thao tác phiên và nhận cập
nhật STOMP. Manager dùng thêm `manager.js` cho catalog, bài viết, đại lý, phụ
kiện và hồ sơ role cấp dưới.

Không gọi trực tiếp port `8091` hoặc `8094` từ frontend. Luôn dùng
`NEXT_PUBLIC_API_BASE_URL`, mặc định cho môi trường local:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:9090
```

## 4. Auth API và Axios

`src/lib/api.js` hiện cung cấp Axios instance chung qua wrapper `apiFetch()`:

- Dùng `NEXT_PUBLIC_API_BASE_URL` làm `baseURL` và bật `withCredentials`.
- Gắn bearer access token bằng request interceptor.
- Khi request protected gặp `401`, response interceptor gọi `POST /user/refresh-token`
  theo cơ chế single-flight, lưu access token mới và retry request một lần.
- Không refresh cho login, register, verify-account, refresh-token hoặc logout.
- Khi refresh thất bại, xóa session client để người dùng đăng nhập lại.

Backend giữ refresh token trong cookie `HttpOnly` và xoay token bằng cache hệ thống in-memory;
frontend không truy cập hoặc lưu refresh token bằng JavaScript.

## 5. Quy tắc React và trạng thái dữ liệu

- Để `page.js` compose layout và component; giữ API calls trong lớp service.
- Chỉ dùng `"use client"` khi component cần state, effect, event browser hoặc
  session storage.
- Form có validation, loading, error, disabled khi submit và ngăn submit lặp.
- Vùng danh sách bất đồng bộ có loading, empty và error state.
- Giữ response handling tương thích với wrapper backend có thể dùng
  `success` hoặc `isSuccess`.
- Upload PDF dùng `FormData`, không ép content type JSON.
- Dùng catalog API hiện có thay vì hard-code danh sách xe.

## 6. Ngôn ngữ UI/UX Tayota

### Hệ thị giác

- Dùng phong cách tối giản, cao cấp với bảng màu đen, trắng và xám chủ đạo.
- Duy trì typography và spacing nhất quán giữa site công khai và dashboard.
- Chỉnh heading theo cấp độ nội dung; tránh thẻ heading quá lớn so với body,
  label và control xung quanh.
- Giữ trạng thái focus, hover, disabled và error rõ ràng, dễ truy cập.

### Workspace theo role

- Mỗi workspace chỉ hiển thị navigation, tab và thao tác phù hợp role.
- Chia chức năng thành tab/panel có tên rõ nghĩa; giữ tab hiện tại nhìn thấy
  được và hỗ trợ keyboard focus.
- Luôn kiểm tra quyền phía backend; việc ẩn nút ở frontend không thay thế
  authorization.

### Layout ổn định

- Đặt chiều cao tối thiểu hợp lý cho panel thay đổi nội dung theo tab, bước
  form, empty/loading/error state.
- Dành sẵn vùng cho validation và thông báo để form không giật khi text xuất
  hiện hoặc biến mất.
- Dùng skeleton hoặc container ổn định cho nội dung load bất đồng bộ.
- Kiểm tra responsive ở chiều rộng nhỏ và chiều cao viewport ngắn; tránh
  overflow làm đổi kích thước form bất ngờ.

### Scrollbar

- Khi chạm vào vùng cuộn, định nghĩa scrollbar phù hợp theme trong
  `globals.css` hoặc scope component hợp lý.
- Bảo đảm thumb/track có tương phản đủ, hover rõ và không làm giảm khả năng
  cuộn trên trình duyệt hỗ trợ.
- Không tạo nhiều thanh cuộn lồng nhau nếu layout có thể dùng một container
  chính.

## 7. Tiếng Việt và comment

- Lưu source và nội dung UI mới bằng UTF-8.
- Hiển thị tiếng Việt có dấu đầy đủ; sửa chuỗi lỗi mã hóa trong vùng đang chạm
  tới thay vì nhân bản lỗi cũ.
- Giữ `lang="vi"` trong root layout.
- Comment mới chỉ dành cho logic khó đọc, viết tiếng Việt có dấu và bắt đầu
  bằng động từ, ví dụ:

```js
// Chuyển người dùng về đăng nhập khi refresh token không còn hợp lệ.
```

## 8. Checklist khi thay frontend

- Đọc `AGENTS.md`, service hiện có và component/page bị tác động.
- Xác định API đi qua gateway prefix nào và role nào được sử dụng.
- Không làm mất single-flight refresh của Axios instance chung khi mở rộng API
  authenticated.
- Soát layout shift, heading, scrollbar, form state và text UTF-8 khi đổi UI.
- Chạy:

```powershell
npm run lint
npm run build
```

- Nếu thay đổi cần backend để kiểm thử tích hợp, dựng backend qua
  `docker compose` từ `tayota-backend`, không khởi chạy service backend rời.
