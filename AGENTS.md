# Tayota - Hướng dẫn bắt buộc cho Agent

## 1. Đọc trước khi làm việc

Đây là tài liệu nguồn chính của repository. Mọi agent phải đọc file này trước
khi khảo sát, lập kế hoạch, chỉnh source code, chạy lệnh hoặc sửa tài liệu.

Đọc thêm tài liệu theo vùng thay đổi:

| Phạm vi | Tài liệu cần đọc thêm |
| --- | --- |
| API Gateway, Java backend, auth, nghiệp vụ | `tayota-backend/AI_CODING.md` |
| AI/RAG, PDF, Qdrant, MongoDB | `tayota-backend/ai-service/README.md` |
| Frontend, layout, UI/UX, client API | `tayota-frontend/UI_UX_IMPLEMENTATION_PLAN.md` |

Nếu tài liệu khác với source code, cấu hình hoặc `docker-compose.yml` hiện tại,
hãy xem source/config/Compose là nguồn sự thật cuối cùng và cập nhật lại tài liệu
khi task có liên quan.

## 2. Trạng thái cần phân biệt rõ

Không mô tả kế hoạch tương lai như tính năng đã hoàn thành.

| Hạng mục | Hiện trạng trong source hiện tại | Hướng thực hiện khi có task liên quan |
| --- | --- | --- |
| Chạy backend | Có `docker-compose.yml` cho gateway, operation, AI và storage. | Chạy và kiểm thử backend thông qua Docker Compose. |
| HTTP client frontend | `src/lib/api.js` đang dùng `fetch` qua `apiFetch()`, có single-flight refresh khi gặp `401`. Package chưa có `axios`. | Khi migrate/sửa luồng authenticated API, giữ hành vi single-flight trong Axios instance chung. |
| Refresh token backend | Đã có `POST /user/refresh-token`; refresh token ở cookie `HttpOnly` và session/hash trong Redis. | Không đưa refresh token ra JavaScript hoặc storage phía client. |
| Tạo tài khoản nội bộ | Backend đã có `POST /user/create-account`, chỉ `ADMIN`; frontend hiện có vùng quản trị gọi endpoint này trong working tree. | Dùng đúng endpoint auth hiện có, không tạo luồng tài khoản song song. |
| Role quản lý | Backend có role `MANAGER`. | Frontend chưa có dashboard riêng cho `MANAGER`; không tuyên bố UI role này đã hoàn chỉnh. |

## 3. Sản phẩm và chức năng

Tayota là nền tảng showroom và vận hành dịch vụ xe, gồm:

- Website giới thiệu, catalog, chi tiết và so sánh xe, thông tin đại lý.
- Đặt lịch lái thử và lịch dịch vụ.
- Đăng ký, xác thực email, đăng nhập, refresh token và hồ sơ người dùng.
- Workspace nghiệp vụ theo quyền.
- Live chat realtime giữa khách hàng và nhân viên.
- AI tư vấn xe dùng RAG và kho tài liệu PDF.
- Thông báo, đánh giá dịch vụ và phiếu sửa chữa.

Các role hiện có trong backend:

`ADMIN`, `MANAGER`, `SERVICE_ADVISOR`, `ASSISTANT`, `MECHANIC`, `USER`.

## 4. Kiến trúc hệ thống

```text
Frontend Next.js
  -> API Gateway :9090
      -> Operation Service :8091 (nội bộ mạng Compose)
           -> PostgreSQL + Redis
           -> WebSocket live chat
      -> AI Service :8094
           -> MongoDB/GridFS + Qdrant + LLM
```

### Thành phần

| Thư mục | Công nghệ | Trách nhiệm |
| --- | --- | --- |
| `tayota-frontend` | Next.js App Router, React | Trang công khai, dashboard, form, session client và API wrappers. |
| `tayota-backend/api-gateway` | Spring Cloud Gateway | Route, CORS, kiểm tra access token, trusted headers và gateway secret. |
| `tayota-backend/operation-service` | Spring Boot, PostgreSQL, Redis | Auth, người dùng, xe, lịch hẹn, chat, work order, notification, review. |
| `tayota-backend/ai-service` | FastAPI, MongoDB/GridFS, Qdrant | RAG chat, lịch sử AI và quản lý tài liệu PDF. |

### Storage

| Storage | Dữ liệu |
| --- | --- |
| PostgreSQL | Dữ liệu nghiệp vụ của operation service. |
| Redis | Session auth, refresh token hash, OTP và trạng thái tạm. |
| MongoDB/GridFS | Session/dữ liệu AI và file PDF. |
| Qdrant | Vector chunks dùng cho RAG. |

## 5. Đường đi của request

Frontend chỉ gọi API Gateway. Không gọi thẳng operation service hoặc AI service
trong luồng ứng dụng.

| Prefix từ frontend | Service đích | Ghi chú |
| --- | --- | --- |
| `/user/**` | Operation Service | Auth, hồ sơ, live chat user/staff. |
| `/car/**` | Operation Service | Catalog, xe, đại lý và phụ kiện. |
| `/operation/**` | Operation Service | Lịch hẹn, thông báo, đánh giá, work order. |
| `/ai/**` | AI Service | AI chat và tài liệu RAG. |
| `/user/chat/ws/**` | Operation Service WebSocket | STOMP/WebSocket live chat. |

Gateway xóa các trusted headers do client gửi lên và tự thiết lập từ JWT. Client
không được tự gắn `X-User-Id`, `X-User-Role`, `X-User-Email`,
`X-AI-Session-Id` hoặc `X-Gateway-Secret`.

Request đến AI service phải đi qua gateway để được gắn
`X-Gateway-Secret`. AI chat hỗ trợ phiên khách bằng cookie session do gateway
quản lý; endpoint quản trị tài liệu vẫn yêu cầu role phù hợp.

## 6. Auth và refresh token

### Luồng backend đã có

| Tác vụ | API gateway path | Hành vi hiện tại |
| --- | --- | --- |
| Tạo tài khoản nội bộ | `POST /user/create-account` | `AuthController.createAccount()` gọi `AuthService.createAccount()`; chỉ `ADMIN`. |
| Đăng ký | `POST /user/register` | Tạo người dùng và luồng xác thực email. |
| Xác thực | `POST /user/verify-account` | Xác thực tài khoản. |
| Đăng nhập | `POST /user/login` | Trả `accessToken` trong body và đặt refresh token vào cookie `HttpOnly`. |
| Refresh | `POST /user/refresh-token` | Đọc cookie, kiểm tra Redis, xoay refresh token/cookie và trả access token mới. |
| Đăng xuất | `POST /user/logout` | Xóa refresh session liên quan và xóa cookie. |

### Quy tắc bảo mật

- Chỉ giữ access token ở session client theo cơ chế hiện hành; tuyệt đối không
  lưu refresh token vào `localStorage`, state React hoặc JavaScript.
- Đánh giá whitelist gateway khi thêm endpoint public mới.
- Giữ endpoint protected phía operation service được bảo vệ bằng quyền phù hợp,
  thường qua `@PreAuthorize`.
- Không tin dữ liệu role/user nhận trực tiếp từ browser; service chỉ tin headers
  đã được gateway xác thực và gắn lại.

## 7. Frontend và chuẩn Axios

### Hiện trạng

Frontend hiện dùng `src/lib/api.js` với `fetch`:

- Đọc access token từ session/local storage hiện có.
- Gửi cookie bằng `credentials: "include"`.
- Gắn `Authorization: Bearer <accessToken>` nếu có.
- Khi request gặp `401`, dùng một refresh promise chung rồi gửi lại request một lần.

Luồng hiện tại chưa phải Axios interceptor, nhưng đã gom các request `401` đồng
thời vào một lần refresh vì backend xoay refresh token. Khi migrate phải bảo
toàn hành vi này.

### Chuẩn bắt buộc khi triển khai Axios

Khi task yêu cầu sửa auth/API authenticated hoặc migrate client HTTP, sử dụng
một Axios instance chung trong `src/lib/api.js` hoặc module tương đương:

- Thêm dependency `axios` theo package manager hiện có của frontend.
- Lấy `baseURL` từ `NEXT_PUBLIC_API_BASE_URL`.
- Đặt `withCredentials: true`.
- Dùng request interceptor để gắn bearer access token.
- Dùng response interceptor để bắt `401` do access token hết hạn.
- Không refresh các request `login`, `register`, `verify-account`,
  `refresh-token` và `logout`.
- Dùng một promise đang chạy hoặc queue để nhiều request lỗi cùng lúc chỉ gọi
  refresh đúng một lần.
- Lưu access token mới và retry request ban đầu tối đa một lần.
- Nếu refresh thất bại, xóa access token/current user và chuyển người dùng ra
  khỏi khu vực yêu cầu đăng nhập.

## 8. Cấu trúc code theo module

### Operation Service

```text
src/main/java/com/tayota/operationservice/
  config/
  controller/
  dto/
  entity/
  enums/
  exception/
  filter/
  mapper/
  object/
  repository/
  service/
  util/
```

- Controller nhận input, validate, gọi service và trả `ApiResponse<T>`.
- Service chứa nghiệp vụ, kiểm tra quyền domain và transaction cho luồng ghi.
- Repository dùng Spring Data JPA; tránh query lặp và N+1 khi mở rộng dữ liệu.
- Không trả entity JPA trực tiếp ra API; dùng DTO/mapper theo package hiện có.
- Kiểm tra `AuthenticationFilter.java` và gateway `application.yml` khi thay API/auth.

### AI Service

```text
ai-service/
  app.py
  rag.py
  vector_database.py
  mongo_storage.py
  conversation_state_manager.py
  chunking.py
  embed.py
  intent_classifier.py
  slot_extractor.py
  business_rules.py
  logic_smart_car_consultant.py
  tests/
```

- `app.py` định nghĩa FastAPI routes, Pydantic models và bảo vệ gateway.
- Dùng Pydantic model cho input/output; không tin role/user header ngoài gateway.
- Trả lỗi phù hợp khi MongoDB, Qdrant hoặc LLM không sẵn sàng, thường là `503`.
- Cập nhật test AI khi sửa endpoint, authorization hoặc luồng tài liệu.

### Frontend

```text
src/
  app/
  components/
  lib/
    api.js
    session.js
    format.js
    services/
```

- Page compose giao diện; đặt lời gọi API trong `src/lib/services`.
- Dùng `"use client"` chỉ khi cần state, effect hoặc browser API.
- Form phải có loading, error và ngăn submit lặp.
- Danh sách async phải có loading, empty và error state.
- Dùng API catalog hiện có thay vì hard-code dữ liệu xe.

## 9. Route frontend đang có

### Public

`/`, `/vehicles`, `/vehicles/[id]`, `/compare`, `/dealerships`,
`/appointments/test-drive`, `/appointments/service`, `/auth/login`,
`/auth/register`, `/verify-account`, `/reviews/[token]`,
`/support/live-chat`.

### Workspace

`/dashboard`, `/dashboard/admin`, `/dashboard/advisor`,
`/dashboard/assistant`, `/dashboard/mechanic`, `/dashboard/user`.

Mapping frontend hiện có chuyển `ADMIN`, `SERVICE_ADVISOR`, `ASSISTANT` và
`MECHANIC` vào dashboard tương ứng. Chưa có route riêng cho `MANAGER`.

## 10. API contract

Spring API thường trả wrapper:

```json
{
  "success": true,
  "code": 200,
  "message": "Thông báo",
  "result": {},
  "timestamp": "..."
}
```

- Frontend cần chịu được cả `success` và `isSuccess` nếu serialization backend
  đang khác nhau giữa các API.
- FastAPI AI dùng response model riêng, không ép theo wrapper Spring.
- Upload PDF dùng `FormData`; không tự đặt `Content-Type: application/json`.
- Phân biệt AI chat `/ai/api/v1/chat` với live chat STOMP/WebSocket của
  operation service.

## 11. UI/UX, tiếng Việt và comment

### UI/UX

- Giữ ngôn ngữ thị giác Tayota tối giản, đen/trắng/xám và đồng nhất giữa public
  site với dashboard.
- Điều chỉnh cấp heading theo hệ typography, không để heading lấn át nội dung.
- Thiết kế scrollbar phù hợp theme thay vì để mặc định khi chỉnh vùng cuộn.
- Giữ kích thước panel/form ổn định khi chuyển tab, loading, validation hoặc
  thông báo lỗi để tránh layout shift.
- Giao diện theo role phải phân tách chức năng rõ ràng bằng tab/navigation và
  chỉ hiển thị thao tác phù hợp quyền.

### UTF-8 và tiếng Việt

- Lưu tài liệu, source và text UI mới bằng UTF-8.
- Viết tiếng Việt có dấu; không biến thành không dấu để tránh lỗi encoding.
- Khi sửa vùng có chuỗi lỗi mã hóa, sửa các chuỗi bị tác động thành tiếng Việt
  đúng thay vì sao chép lỗi cũ.
- Giữ `lang="vi"` trong root layout frontend.
- Trước khi bàn giao, tìm dấu hiệu mojibake trong file source/UI đã sửa, chẳng
  hạn các chuỗi chứa `Ã`, `Ä`, `Æ` hoặc `áº`.

### Comment

- Comment mới bằng tiếng Việt có dấu và chỉ thêm khi mô tả nghiệp vụ, bảo mật
  hoặc logic khó đọc.
- Bắt đầu comment bằng động từ hoặc cụm động từ:
  - Đúng: `// Lấy thông tin người dùng từ security context.`
  - Đúng: `// Kiểm tra refresh token còn hợp lệ trong Redis.`
  - Đúng: `// Tạo cookie HttpOnly chứa refresh token mới.`
  - Không dùng: `// User`, `// Validation`, `// Refresh token`.

## 12. Backend chỉ chạy qua Docker Compose

Thư mục vận hành backend chuẩn là `tayota-backend`. Không hướng dẫn agent chạy
riêng operation service, gateway hoặc AI service trên máy host làm quy trình
mặc định.

### Service và port

| Container | Port |
| --- | --- |
| `api-gateway` | `9090` xuất ra host |
| `operation-service` | `8091` trong mạng Compose |
| `ai-service` | `8094` |
| `postgres-operation-service` | `5432` |
| `redis` | `6379` |
| `mongodb` | `27017` |
| `qdrant` | `6333`, `6334` |

### Lệnh vận hành chuẩn

```powershell
cd tayota-backend
docker compose up --build
```

```powershell
cd tayota-backend
docker compose logs -f api-gateway operation-service ai-service
```

```powershell
cd tayota-backend
docker compose down
```

Frontend chạy riêng bằng Next.js và gọi gateway qua:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:9090
```

### Quy tắc kiểm thử backend

Mọi test hoặc lệnh thực thi thuộc backend cũng phải chạy trong Docker Compose.
Compose hiện tại xây runtime image Java với bước package bỏ qua test và chưa
cung cấp service test chuyên dụng. Vì vậy:

- Không chạy Maven, Java server hoặc Python AI backend trực tiếp trên host để
  báo cáo xác minh chuẩn.
- Nếu task cần chạy test backend, trước tiên kiểm tra hoặc bổ sung compose
  profile/override/service test phù hợp với module rồi chạy qua
  `docker compose`.
- Nếu task chỉ sửa tài liệu và không dựng container, ghi rõ chưa chạy test
  runtime vì thay đổi không tác động hành vi.

## 13. Checklist làm việc

### Trước khi sửa

- Đọc `AGENTS.md` và tài liệu module liên quan.
- Kiểm tra `git status`; không ghi đè thay đổi chưa thuộc task.
- Xác định đường request qua gateway, trạng thái public/protected và role.
- Xác định tác động tới auth, cookie, Redis hoặc storage khác.
- Kiểm tra vùng text tiếng Việt sẽ chỉnh có lỗi mã hóa hay không.

### Trong khi sửa

- Bám theo helper/service/package hiện có.
- Phân biệt rõ tính năng đã tồn tại với yêu cầu đang triển khai.
- Giữ refresh token trong cookie `HttpOnly`.
- Viết comment UTF-8, có dấu và bắt đầu bằng động từ.
- Không thay đổi quy trình backend ra khỏi Docker Compose.

### Trước khi bàn giao

- Khi đổi frontend: chạy `npm run lint` và `npm run build`.
- Khi đổi backend/AI: chạy xác minh cần thiết qua Docker Compose hoặc nêu rõ
  compose test setup chưa có nếu đó là blocker.
- Soát text tiếng Việt vừa sửa và các liên kết/tài liệu chịu ảnh hưởng.
- Nêu rõ file đã đổi, xác minh đã chạy và phần chưa chạy được.
