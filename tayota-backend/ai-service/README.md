# Tayota AI Service - Hướng dẫn cho Agent

> Đọc `../../AGENTS.md` trước khi thay đổi AI service. AI service là một phần
> của backend và được chạy/kiểm thử theo quy trình Docker Compose. Frontend chỉ
> gọi AI thông qua API Gateway, không gọi trực tiếp service trong luồng ứng
> dụng.

## 1. Vai trò trong hệ thống

AI service là ứng dụng FastAPI cung cấp:

- Chat tư vấn xe dùng RAG.
- Quản lý lịch sử phiên hội thoại AI.
- Upload, lập chỉ mục, liệt kê trạng thái và xóa tài liệu PDF cho Admin.
- Health check cho hạ tầng AI.

```text
Frontend
  -> API Gateway /ai/**
      -> AI Service :8094
          -> MongoDB/GridFS: tài liệu, metadata và session
          -> Qdrant: vector chunks
          -> LLM/Embedding provider
```

## 2. Source map

```text
ai-service/
  app.py                         # FastAPI routes, middleware và models
  rag.py                         # Luồng truy xuất/sinh câu trả lời
  vector_database.py             # Kết nối và thao tác Qdrant
  mongo_storage.py               # MongoDB/GridFS
  conversation_state_manager.py  # Trạng thái hội thoại
  chunking.py                    # Chia tài liệu
  embed.py                       # Embedding
  intent_classifier.py           # Phân loại ý định
  slot_extractor.py              # Trích xuất dữ liệu tư vấn
  business_rules.py              # Luật nghiệp vụ
  logic_smart_car_consultant.py  # Điều phối tư vấn
  tests/
```

Đọc `app.py` trước khi đổi endpoint, gateway authorization, request/response
schema hoặc document job. Đọc các module storage/vector/RAG tương ứng trước khi
đổi indexing hoặc retrieval.

## 3. Security và đường gọi API

Gateway route frontend:

| Frontend gọi | AI route sau khi gateway bỏ prefix |
| --- | --- |
| `/ai/health` | `/health` |
| `/ai/api/v1/chat` | `/api/v1/chat` |
| `/ai/api/v1/documents...` | `/api/v1/documents...` |

Quy tắc bắt buộc:

- `app.py` kiểm tra `X-Gateway-Secret` cho `/health` và `/api/v1/*`.
- Không tin `X-User-Id` hoặc `X-User-Role` từ request đi thẳng vào AI service;
  các header này chỉ hợp lệ sau gateway.
- AI chat có thể hoạt động cho khách thông qua AI session cookie/header do
  gateway thiết lập.
- Endpoint quản trị tài liệu yêu cầu role `ADMIN` theo header tin cậy từ
  gateway.
- Không để frontend tự gắn `X-Gateway-Secret`, role hoặc user id.

## 4. Nhóm endpoint

Tên endpoint cụ thể phải được đối chiếu trong `app.py` trước khi sửa client
hoặc tài liệu API.

| Nhóm | Mục đích | Quyền/ngữ cảnh |
| --- | --- | --- |
| `/health` | Kiểm tra trạng thái AI dependencies. | Qua gateway. |
| `/api/v1/chat` | Nhận câu hỏi và trả tư vấn RAG. | Khách hoặc người dùng đăng nhập qua gateway. |
| `/api/v1/sessions...` | Lịch sử/quản lý phiên chat AI. | Theo phiên hoặc người dùng đã xác thực. |
| `/api/v1/documents...` | Upload, liệt kê, theo dõi job và xóa PDF. | Admin qua gateway. |

### Chat AI và live chat là hai tính năng khác nhau

- AI chat: `/ai/api/v1/chat`, xử lý bởi FastAPI/RAG.
- Live chat nhân viên: `/user/chat/*` và `/user/chat/ws/*`, xử lý bởi operation
  service/STOMP WebSocket.

Không trộn response model, session hoặc websocket của hai luồng này.

## 5. Luồng tài liệu RAG

Khi Admin tải tài liệu PDF qua gateway:

1. Nhận `multipart/form-data` ở endpoint document.
2. Kiểm tra request đến từ gateway và role được phép.
3. Lưu file/metadata cần thiết trong MongoDB/GridFS.
4. Chia văn bản thành chunks và sinh embedding.
5. Lưu vector chunks trong Qdrant.
6. Cập nhật trạng thái job để frontend theo dõi.
7. Xóa đồng bộ metadata/file/vector khi thực hiện delete.

Khi sửa luồng này:

- Dùng Pydantic model phù hợp cho body/response không phải file upload.
- Giữ upload bằng `FormData`; không yêu cầu frontend gửi JSON content type.
- Xử lý lỗi MongoDB, Qdrant hoặc provider bằng HTTP status phù hợp, thường là
  `503` khi dependency không khả dụng.
- Cập nhật test trong `tests/` cho authorization, validation và job behavior bị
  tác động.

## 6. Cấu hình và chạy bằng Docker Compose

AI service không phải ứng dụng backend độc lập trong quy trình chuẩn. Dựng
toàn bộ backend từ thư mục cha:

```powershell
cd tayota-backend
docker compose up --build
```

Theo dõi log service:

```powershell
cd tayota-backend
docker compose logs -f ai-service api-gateway
```

Dừng backend:

```powershell
cd tayota-backend
docker compose down
```

Compose kết nối AI service với MongoDB và Qdrant, đồng thời cung cấp secret/cấu
hình cần thiết theo file environment và Compose hiện tại. Khi thêm biến môi
trường mới, cập nhật cấu hình container và tài liệu liên quan cùng một task.

## 7. Kiểm thử và quy tắc code

### Kiểm thử

- Test hiện có nằm trong `ai-service/tests`.
- Backend chỉ được xác minh runtime/test qua Docker Compose.
- Compose hiện chưa định nghĩa service test chuyên dụng; nếu task yêu cầu chạy
  pytest AI, hãy thêm hoặc dùng một Compose override/profile test phù hợp rồi
  chạy trong container, không dùng Python host làm kết quả xác minh chuẩn.

### Code và comment

- Dùng model Pydantic cho input/output khi route không phải upload stream.
- Giữ route mỏng; đưa logic RAG, storage và business rules về module tương ứng.
- Viết comment/docstring mới bằng tiếng Việt có dấu, lưu UTF-8 và bắt đầu bằng
  động từ khi mô tả hành động xử lý.
- Chỉ comment ràng buộc bảo mật, nghiệp vụ hoặc logic không tự hiển nhiên.
- Sửa chuỗi tiếng Việt lỗi mã hóa trong phạm vi source/tài liệu đang thay đổi.

## 8. Checklist trước bàn giao

- Đã đọc `AGENTS.md` và các module AI chịu ảnh hưởng.
- Đã kiểm tra endpoint luôn được frontend gọi qua `/ai/**` ở gateway.
- Đã giữ kiểm tra gateway secret và quyền Admin cho tài liệu.
- Đã kiểm tra đồng bộ MongoDB/GridFS/Qdrant khi đổi document flow.
- Đã chạy test qua Compose nếu có cấu hình phù hợp, hoặc nêu rõ chưa có test
  runner Compose trong phạm vi task.
- Đã soát UTF-8/comment và cập nhật tài liệu nếu contract thay đổi.
