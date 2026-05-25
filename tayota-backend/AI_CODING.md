# Tayota Backend - Hướng dẫn cho Agent

> Đọc `../AGENTS.md` trước khi thay đổi API Gateway hoặc operation service.
> File này chỉ bổ sung chi tiết Java/backend. Quy trình chạy và kiểm thử backend
> chuẩn phải đi qua Docker Compose từ thư mục `tayota-backend`.

## 1. Phạm vi và nguồn sự thật

Backend nghiệp vụ gồm hai ứng dụng Java:

| Module | Trách nhiệm |
| --- | --- |
| `api-gateway` | Nhận request frontend, CORS, xác minh JWT, route, trusted headers và gateway secret. |
| `operation-service` | Auth, user, catalog xe, lịch hẹn, live chat, thông báo, đánh giá và work order. |

Khi chỉnh API/auth, đọc trực tiếp tối thiểu các file sau:

```text
api-gateway/src/main/resources/application.yml
api-gateway/src/main/java/com/tayota/apigateway/filter/AuthenticationFilter.java
operation-service/src/main/java/com/tayota/operationservice/controller/
operation-service/src/main/java/com/tayota/operationservice/service/
operation-service/src/main/java/com/tayota/operationservice/config/SecurityConfig.java
```

Không giả định một service riêng cho live chat: live chat hiện nằm trong
`operation-service`.

## 2. Docker Compose là cách chạy backend

Backend được dựng từ `docker-compose.yml`:

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

| Service | Kết nối hiện tại |
| --- | --- |
| `api-gateway` | Public qua `localhost:9090`. |
| `operation-service` | Port `8091` trong mạng Compose; frontend không gọi trực tiếp. |
| `ai-service` | Port `8094`, nhưng frontend vẫn gọi qua gateway. |
| `postgres-operation-service`, `redis`, `mongodb`, `qdrant` | Dependency backend trong Compose. |

Dockerfile Java hiện package với `-DskipTests`. Khi cần kiểm thử operation
service, dùng service `operation-service-test` trong `docker-compose.test.yml`,
không chạy Maven hay backend service trực tiếp trên host rồi coi đó là xác
minh chuẩn:

```powershell
docker compose -f docker-compose.yml -f docker-compose.test.yml --profile test run --rm --no-deps operation-service-test mvn -B test
```

## 3. Gateway và luồng xác thực

### Route

| Gateway path | Đích |
| --- | --- |
| `/user/**` | Operation service, gồm auth/profile/chat. |
| `/car/**` | Operation service, gồm catalog/dealership/accessory. |
| `/operation/**` | Operation service, gồm appointment/notification/review/workorder. |
| `/user/chat/ws/**` | Operation service WebSocket. |
| `/ai/**` | AI service. |

Gateway dùng `StripPrefix=1`, vì vậy controller operation service không nhận
phần prefix gateway.

### `AuthenticationFilter`

Filter hiện chịu trách nhiệm:

- Cho qua các endpoint public được whitelist, gồm auth public, catalog công
  khai, đặt lịch khách, review token, AI health và một số đường chat.
- Kiểm tra bearer access token cho request protected.
- Xóa trusted headers nhận từ browser rồi gắn lại thông tin lấy từ JWT:
  `X-User-Id`, `X-User-Role`, `X-User-Email`.
- Gắn `X-Gateway-Secret` cho request AI.
- Quản lý `X-AI-Session-Id`/cookie cho AI chat khách; token đăng nhập là tùy
  chọn ở luồng AI chat.

Khi thêm endpoint:

1. Xác định endpoint là public hay protected.
2. Chọn đúng prefix gateway; không để frontend gọi port service nội bộ.
3. Cập nhật whitelist chỉ khi endpoint thực sự public.
4. Kiểm tra quyền ở operation service cho endpoint nghiệp vụ nhạy cảm.

## 4. Operation service

### Cấu trúc package

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

### Domain và API group

| Domain | Gateway group | Chức năng |
| --- | --- | --- |
| Auth/user | `/user/*` | Tạo tài khoản, đăng ký, login, refresh, logout, profile và thiết bị. |
| Live chat | `/user/chat/*`, `/user/assistant/chat/*`, `/user/chat/ws/*` | Hội thoại realtime khách hàng và nhân viên. |
| Xe | `/car/*` | Catalog, thông số, so sánh, đại lý, phụ kiện và quản trị xe. |
| Appointment | `/operation/appointments/*` | Đặt lịch khách/đăng nhập và quản trị lịch của advisor. |
| Notification | `/operation/notifications/*` | Thông báo người dùng. |
| Review | `/operation/reviews/*` | Đánh giá bằng token và lịch sử đánh giá. |
| Work order | `/operation/workorders/*` | Phiếu sửa chữa cho mechanic. |

### Quy chuẩn Java

- Controller nhận request/validate, gọi service và trả `ApiResponse<T>`;
  không đưa nghiệp vụ dài vào controller.
- Dùng `@PreAuthorize` cho endpoint cần kiểm soát role.
- Service chứa nghiệp vụ, kiểm tra quyền theo domain và dùng `@Transactional`
  cho luồng ghi cần tính nhất quán.
- Ném `CustomException` theo cơ chế lỗi đang có cho lỗi nghiệp vụ.
- Repository dùng Spring Data JPA và tránh N+1 khi bổ sung truy vấn dữ liệu.
- Trả DTO qua mapper; không đưa entity JPA trực tiếp ra API.
- Giữ naming/package theo module đang sửa, không đổi tên diện rộng chỉ để
  đồng nhất hình thức.

## 5. Auth, role và refresh token

### Endpoint đã triển khai

| Gateway API | Trạng thái/quyền |
| --- | --- |
| `POST /user/create-account` | Đã có; chỉ `ADMIN`; gọi `AuthService.createAccount()`. |
| `POST /user/register` | Public; đăng ký tài khoản. |
| `POST /user/verify-account` | Public; xác thực email/tài khoản. |
| `POST /user/login` | Public; trả access token và đặt refresh cookie. |
| `POST /user/refresh-token` | Public theo nghĩa không cần bearer; đọc cookie `HttpOnly`, xoay token bằng Redis. |
| `POST /user/logout` | Xóa session/cookie refresh liên quan. |
| `POST /user/logout-all` | Xóa các phiên đăng nhập theo logic service. |
| `PATCH /user/admin/users/{userId}/password` | Admin đặt lại mật khẩu tài khoản cấp dưới qua `AuthService`; thu hồi toàn bộ phiên của tài khoản đích. |
| `PATCH /user/admin/users/{userId}/dealership` | Admin đổi đại lý đang hoạt động cho `SERVICE_ADVISOR` hoặc `MECHANIC` cấp dưới. |

### Chi tiết cần bảo toàn

- Access token truyền về frontend trong response body sau login/refresh.
- Refresh token chỉ tồn tại dưới dạng cookie `HttpOnly` phía browser và dữ
  liệu session/hash phía Redis.
- `create-account` kiểm tra quyền role; luồng tạo `SERVICE_ADVISOR` hoặc
  `MECHANIC` yêu cầu dữ liệu đại lý theo service hiện có.
- DTO danh sách/chi tiết tài khoản admin trả cả `loginProvider` để UI phân biệt
  tài khoản đăng nhập nội bộ và tài khoản Google.
- Ban/unban người dùng hiện cho phép `ADMIN` hoặc `MANAGER`.
- Admin reset mật khẩu không cho phép thao tác trên chính mình hoặc tài khoản `ADMIN` ngang quyền.
- Nhóm endpoint assistant chat được bảo vệ cho các role vận hành phù hợp
  (`ADMIN`, `MANAGER`, `ASSISTANT`, `SERVICE_ADVISOR`) theo controller hiện tại.
- Manager quản trị nội dung website qua `/manager/**` thuộc nhóm `/car/*` và
  xem/thống kê role cấp dưới qua `/manager/users/**` thuộc nhóm `/user/*`;
  không có báo cáo doanh thu hoặc thao tác bảo mật tài khoản trong phạm vi này.

Khi đổi auth, phải kiểm tra đồng thời gateway filter, controller, service,
cookie, Redis và client frontend.

## 6. Comment, UTF-8 và API response

- Viết comment mới bằng tiếng Việt có dấu, lưu UTF-8.
- Bắt đầu comment nghiệp vụ bằng động từ hoặc cụm động từ:

```java
// Kiểm tra refresh token còn hợp lệ trong Redis.
// Tạo cookie HttpOnly chứa refresh token mới.
```

- Không thêm comment chỉ lặp lại cú pháp Java.
- Khi sửa thông báo tiếng Việt có dấu hiệu lỗi mã hóa, chuẩn hóa nội dung trong
  phạm vi task.

Operation service thường trả wrapper:

```json
{
  "success": true,
  "code": 200,
  "message": "Thông báo",
  "result": {},
  "timestamp": "..."
}
```

## 7. Kiểm tra khi thay backend

- Kiểm tra route gateway và whitelist đối với endpoint mới/đổi quyền.
- Kiểm tra không có client-controlled trusted header đi qua gateway.
- Kiểm tra auth thay đổi không làm lộ refresh token hoặc bỏ qua Redis session.
- Chạy test backend cần thiết qua Docker Compose khi đã có cấu hình test phù
  hợp; báo rõ nếu hiện chưa có Compose test runner cho lệnh cần chạy.
- Nếu thay AI service, đọc thêm `ai-service/README.md`.
