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
    api.js              # HTTP helper hiện dùng fetch
    session.js          # Access token, current user và dashboard mapping
    format.js
    services/           # API wrapper theo domain
```

`package.json` hiện có Next.js, React và STOMP client cho realtime chat.
Dependency `axios` chưa tồn tại tại thời điểm tài liệu này được đối chiếu.

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
| `/dashboard/advisor` | Service advisor. |
| `/dashboard/assistant` | Assistant. |
| `/dashboard/mechanic` | Mechanic. |
| `/dashboard/user` | Người dùng mặc định. |

Backend có role `MANAGER`, nhưng frontend hiện chưa có dashboard riêng cho role
này; mapping session rơi về dashboard mặc định nếu không thuộc các role đã map.

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
| `admin.js` khi hiện diện trong working tree | `/user/create-account`, `/ai/api/v1/documents*`. |

Không gọi trực tiếp port `8091` hoặc `8094` từ frontend. Luôn dùng
`NEXT_PUBLIC_API_BASE_URL`, mặc định cho môi trường local:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:9090
```

## 4. Auth API: hiện trạng và chuẩn Axios

### Hiện trạng `apiFetch()`

`src/lib/api.js` hiện:

- Dùng `fetch`, không dùng Axios.
- Gửi cookie bằng `credentials: "include"`.
- Gắn bearer access token từ session client khi có.
- Gọi `POST /user/refresh-token` khi gặp `401`, gom refresh đồng thời bằng
  single-flight promise, lưu access token mới và thử request ban đầu thêm một lần.

Backend đã có refresh token trong cookie `HttpOnly` và xoay token bằng Redis.
Frontend hiện đã tránh xung đột refresh đồng thời; phải giữ đặc tính này khi
chuyển sang Axios interceptor.

### Khi thực hiện migrate Axios

Axios là chuẩn mục tiêu cho thay đổi client API/auth mới, nhưng không được ghi
nhận là đã triển khai trước khi source thực sự được migrate.

Yêu cầu triển khai:

- Cài `axios` và tạo một instance dùng chung ở `src/lib/api.js` hoặc module API
  tương đương.
- Dùng `baseURL: process.env.NEXT_PUBLIC_API_BASE_URL` và
  `withCredentials: true`.
- Gắn access token bằng request interceptor.
- Bắt `401` bằng response interceptor; bỏ qua refresh cho login, register,
  verify-account, refresh-token và logout.
- Dùng single-flight promise hoặc queue để mọi request đang chờ dùng cùng một
  lần refresh.
- Retry request lỗi đúng một lần; tránh vòng lặp interceptor.
- Khi refresh thất bại, xóa session client và đưa người dùng khỏi route cần
  đăng nhập.
- Không lưu hoặc đọc refresh token bằng JavaScript.

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
- Không tuyên bố Axios đã có nếu chưa sửa source và dependency; không làm mất
  single-flight refresh đang có trong `apiFetch()`.
- Soát layout shift, heading, scrollbar, form state và text UTF-8 khi đổi UI.
- Chạy:

```powershell
npm run lint
npm run build
```

- Nếu thay đổi cần backend để kiểm thử tích hợp, dựng backend qua
  `docker compose` từ `tayota-backend`, không khởi chạy service backend rời.
