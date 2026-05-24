# Tayota AI Service

AI Service là microservice FastAPI dùng cho trợ lý tư vấn xe Toyota trong hệ
thống Tayota. Service nhận câu hỏi từ người dùng, quản lý phiên hội thoại bằng
MongoDB, truy xuất tài liệu PDF đã vector hóa trong Qdrant và sinh câu trả lời
bằng LLM.

## Chức năng chính

- Tư vấn xe Toyota theo ngữ cảnh hội thoại.
- Phân loại ý định, trích xuất nhu cầu như ngân sách, số chỗ, nhiên liệu, khu vực.
- Lưu trạng thái phiên chat và lịch sử tin nhắn vào MongoDB.
- Nhận file PDF, lưu bản gốc vào MongoDB GridFS và index nội dung vào Qdrant.
- Kiểm tra tình trạng MongoDB, Qdrant và cấu hình LLM.

## Thành phần sử dụng

- `FastAPI`: cung cấp REST API.
- `MongoDB`: lưu phiên chat, log tin nhắn, metadata tài liệu và file PDF qua GridFS.
- `Qdrant`: lưu embedding/chunk của tài liệu để truy xuất RAG.
- `sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2`: tạo embedding 384 chiều.
- `Groq`, `Gemini` hoặc `Ollama`: provider sinh câu trả lời, chọn bằng `LLM_PROVIDER`.

## Chạy cùng Tayota Backend

Cấu hình được lấy từ `../.env` và `../docker-compose.yml`.

```powershell
cd Tayota\tayota-backend
docker compose up --build
```

Khi chạy qua Spring Cloud Gateway, dùng base URL:

```text
http://localhost:9090/ai
```

Khi gọi trực tiếp AI Service, dùng base URL:

```text
http://localhost:8094
```

Tài liệu OpenAPI của service có tại:

```text
http://localhost:8094/docs
```

## Danh sách API

### `GET /health`

Kiểm tra tình trạng hoạt động của AI Service và các dependency.

**Chức năng**

- Ping MongoDB.
- Kiểm tra collection Qdrant.
- Kiểm tra trạng thái cấu hình LLM.
- Trả về `ok` nếu MongoDB và Qdrant đều hoạt động, ngược lại trả về `degraded`.

**Response mẫu**

```json
{
  "status": "ok",
  "qdrant": "ok",
  "mongo": "ok",
  "redis": "unused",
  "llm": "configured"
}
```

**Gọi qua gateway**

```text
GET http://localhost:9090/ai/health
```

### `POST /api/v1/chat`

Gửi tin nhắn của người dùng tới trợ lý tư vấn xe Toyota.

**Chức năng**

- Nhận câu hỏi/tin nhắn từ người dùng.
- Dùng `X-AI-Session-Id` để khôi phục hoặc tạo phiên hội thoại.
- Phân loại ý định hội thoại, cập nhật stage và slot nhu cầu.
- Truy xuất tài liệu liên quan trong Qdrant.
- Sinh câu trả lời bằng LLM.
- Trả về câu trả lời kèm nguồn tài liệu tham khảo.

**Header**

| Tên | Bắt buộc | Mô tả |
| --- | --- | --- |
| `Content-Type: application/json` | Có | Body dạng JSON. |
| `X-AI-Session-Id` | Có | ID phiên chat. Cùng một session sẽ giữ ngữ cảnh hội thoại. |
| `X-User-Id` | Không | ID người dùng đăng nhập. Có thể bỏ trống cho khách. |

**Request body**

```json
{
  "message": "Tư vấn xe Toyota 7 chỗ khoảng 1 tỷ"
}
```

`message` là bắt buộc và không được rỗng.

**Response**

```json
{
  "answer": "Với nhu cầu 7 chỗ khoảng 1 tỷ, anh/chị có thể cân nhắc...",
  "sources": [
    {
      "source": "innova-cross.pdf",
      "page": 2,
      "score": 0.82,
      "chunk_id": "innova-cross-...",
      "chunk_index": 4,
      "document_id": "uuid",
      "gridfs_file_id": "object-id"
    }
  ],
  "intent": "car_consultation",
  "stage": "consulting",
  "slots": {
    "budget": 1000000000,
    "seats": 7
  },
  "session_id": "session-1"
}
```

**Ví dụ curl**

```powershell
curl -X POST http://localhost:9090/ai/api/v1/chat `
  -H "Content-Type: application/json" `
  -H "X-AI-Session-Id: session-1" `
  -H "X-User-Id: user-1" `
  -d "{\"message\":\"Tư vấn xe Toyota 7 chỗ khoảng 1 tỷ\"}"
```

**Lỗi thường gặp**

- `422`: thiếu `X-AI-Session-Id`, thiếu `message` hoặc `message` rỗng.
- `503`: MongoDB, Qdrant hoặc LLM provider không sẵn sàng.

### `GET /api/v1/sessions/{session_id}/messages`

Lấy lịch sử chat đã lưu trong collection `ai_chat_messages`.

**Chức năng**

- Lấy các lượt hỏi/đáp đã được log theo `session_id`.
- Trả về dữ liệu theo thứ tự thời gian tăng dần.
- Hỗ trợ phân trang bằng `limit` và `offset`.

**Path param**

| Tên | Mô tả |
| --- | --- |
| `session_id` | ID phiên chat cần lấy lịch sử. |

**Query params**

| Tên | Bắt buộc | Mặc định | Mô tả |
| --- | --- | --- | --- |
| `limit` | Không | `50` | Số message log cần lấy. Tối thiểu `1`, tối đa `200`. |
| `offset` | Không | `0` | Số message log bỏ qua từ đầu danh sách. |

**Response**

```json
{
  "session_id": "session-1",
  "count": 1,
  "messages": [
    {
      "session_id": "session-1",
      "user_id": "user-1",
      "question": "Xe nào 7 chỗ?",
      "answer": "Toyota có một số lựa chọn 7 chỗ phù hợp...",
      "intent": "car_advice",
      "stage": "advising",
      "slots_snapshot": {
        "seats": 7
      },
      "sources": [
        {
          "source": "toyota.pdf",
          "page": 1
        }
      ],
      "model_used": "llama-3.3-70b-versatile",
      "rule_triggered": "",
      "created_at": "2026-05-24T00:00:00Z"
    }
  ]
}
```

**Ví dụ curl**

```powershell
curl "http://localhost:9090/ai/api/v1/sessions/session-1/messages?limit=50&offset=0"
```

**Lỗi thường gặp**

- `422`: `limit` hoặc `offset` không hợp lệ.
- `503`: không đọc được lịch sử chat từ MongoDB.

### `GET /api/v1/users/{user_id}/sessions`

Lấy danh sách session chat của một người dùng.

**Chức năng**

- Phục vụ màn hình lịch sử cuộc trò chuyện theo user.
- Đọc dữ liệu từ collection `ai_sessions`.
- Trả về session mới cập nhật gần nhất trước.
- Hỗ trợ phân trang bằng `limit` và `offset`.

**Path param**

| Tên | Mô tả |
| --- | --- |
| `user_id` | ID người dùng cần lấy danh sách session. |

**Query params**

| Tên | Bắt buộc | Mặc định | Mô tả |
| --- | --- | --- | --- |
| `limit` | Không | `50` | Số session cần lấy. Tối thiểu `1`, tối đa `200`. |
| `offset` | Không | `0` | Số session bỏ qua từ đầu danh sách. |

**Response**

```json
{
  "user_id": "user-1",
  "count": 1,
  "sessions": [
    {
      "session_id": "session-1",
      "user_id": "user-1",
      "stage": "advising",
      "turn_count": 3,
      "last_intent": "car_advice",
      "filled_slots": {
        "seats": 7
      },
      "history_len": 6,
      "status": "active",
      "created_at": "2026-05-24T00:00:00Z",
      "updated_at": "2026-05-24T00:10:00Z"
    }
  ]
}
```

**Ví dụ curl**

```powershell
curl "http://localhost:9090/ai/api/v1/users/user-1/sessions?limit=50&offset=0"
```

**Lỗi thường gặp**

- `422`: `limit` hoặc `offset` không hợp lệ.
- `503`: không đọc được danh sách session từ MongoDB.

### `GET /api/v1/documents`

Lấy danh sách tài liệu đã upload hoặc đã index.

**Chức năng**

- Phục vụ màn hình quản lý tài liệu.
- Đọc metadata tài liệu từ collection `ai_documents`.
- Mặc định trả các trạng thái `uploaded`, `indexing`, `indexed`, `failed`.
- Hỗ trợ lọc trạng thái bằng query param `status`.

**Query params**

| Tên | Bắt buộc | Mặc định | Mô tả |
| --- | --- | --- | --- |
| `status` | Không | `uploaded`, `indexing`, `indexed`, `failed` | Có thể truyền nhiều lần, ví dụ `?status=indexed&status=failed`. |

**Response**

```json
{
  "count": 1,
  "documents": [
    {
      "document_id": "document-uuid",
      "filename": "toyota.pdf",
      "status": "indexed",
      "content_type": "application/pdf",
      "size_bytes": 123456,
      "sha256": "file-sha256",
      "uploaded_by_user_id": "user-1",
      "uploaded_at": "2026-05-24T00:00:00Z",
      "updated_at": "2026-05-24T00:01:00Z"
    }
  ]
}
```

**Ví dụ curl**

```powershell
curl "http://localhost:9090/ai/api/v1/documents"
```

Lọc theo trạng thái:

```powershell
curl "http://localhost:9090/ai/api/v1/documents?status=indexed&status=failed"
```

**Lỗi thường gặp**

- `503`: không đọc được danh sách tài liệu từ MongoDB.

### `POST /api/v1/documents`

Upload PDF để đưa vào kho tri thức RAG.

API này chỉ cho phép Admin xử lý. Khi gọi qua gateway, request phải có
`Authorization: Bearer <admin-access-token>` để gateway set `X-User-Role=ROLE_ADMIN`
và `X-User-Id` xuống AI Service.

**Chức năng**

- Nhận file PDF từ form upload.
- Chỉ chấp nhận file có đuôi `.pdf`.
- Lưu file gốc vào MongoDB GridFS.
- Lưu metadata tài liệu vào collection `ai_documents`.
- Tạo job index tài liệu vào collection `ai_document_jobs`.
- Chạy background job để tách trang, chia chunk, tạo embedding và upsert vào Qdrant.

**Form data**

| Tên | Bắt buộc | Kiểu | Mô tả |
| --- | --- | --- | --- |
| `file` | Có | File | File PDF cần upload. |
| `rebuild` | Không | Boolean query param | Nếu `true`, rebuild lại collection Qdrant từ toàn bộ PDF đã lưu trong MongoDB. Mặc định `false`. |

**Header**

| Tên | Bắt buộc | Mô tả |
| --- | --- | --- |
| `Authorization: Bearer <token>` | Có | Access token của tài khoản Admin khi gọi qua gateway. |
| `X-User-Id` | Có khi gọi nội bộ | ID admin upload tài liệu, do gateway set từ JWT. |
| `X-User-Role` | Có khi gọi nội bộ | Phải chứa `ADMIN` hoặc `ROLE_ADMIN`, do gateway set từ JWT. |

**Response**

```json
{
  "job_id": "7c2e3f8a-...",
  "status": "queued"
}
```

**Ví dụ curl**

```powershell
curl -X POST "http://localhost:9090/ai/api/v1/documents?rebuild=false" `
  -H "Authorization: Bearer <admin-access-token>" `
  -F "file=@C:\docs\toyota.pdf" `
```

**Lỗi thường gặp**

- `400`: file upload không phải PDF.
- `403`: tài khoản không có quyền Admin.
- `422`: thiếu trường `file` trong multipart form.
- `503`: không lưu được file/job vào MongoDB.

### `GET /api/v1/documents/jobs/{job_id}`

Lấy trạng thái job index tài liệu.

**Chức năng**

- Kiểm tra tiến trình xử lý PDF sau khi upload.
- Cho biết job đang chờ, đang chạy, thành công hoặc thất bại.
- Trả về số chunk đã index nếu job hoàn tất.

**Path param**

| Tên | Mô tả |
| --- | --- |
| `job_id` | ID job nhận được từ API upload tài liệu. |

**Response**

```json
{
  "job_id": "7c2e3f8a-...",
  "status": "success",
  "message": "Document indexed successfully.",
  "document_id": "document-uuid",
  "indexed_pages": 0,
  "indexed_chunks": 125
}
```

`status` có thể là:

- `queued`: job đã được tạo và đang chờ chạy.
- `running`: job đang index tài liệu.
- `success`: index thành công.
- `failed`: index thất bại, xem chi tiết trong `message`.

**Ví dụ curl**

```powershell
curl http://localhost:9090/ai/api/v1/documents/jobs/7c2e3f8a-...
```

**Lỗi thường gặp**

- `404`: không tìm thấy job.
- `503`: không đọc được trạng thái job từ MongoDB.

### `DELETE /api/v1/documents/{document_id}`

Xóa một tài liệu khỏi kho tri thức RAG.

API này chỉ cho phép Admin xử lý. Khi gọi qua gateway, request phải có
`Authorization: Bearer <admin-access-token>` để gateway set `X-User-Role=ROLE_ADMIN`
xuống AI Service.

**Chức năng**

- Kiểm tra tài liệu có tồn tại trong MongoDB hay không.
- Xóa các vector/chunk trong Qdrant có payload `document_id` tương ứng.
- Cập nhật lại summary chunk dùng cho câu hỏi danh sách xe.
- Xóa file PDF gốc khỏi MongoDB GridFS.
- Xóa metadata tài liệu khỏi collection `ai_documents`.

**Path param**

| Tên | Mô tả |
| --- | --- |
| `document_id` | ID tài liệu cần xóa. Có thể lấy từ response nguồn của API chat hoặc trạng thái job upload. |

**Response**

```json
{
  "document_id": "document-uuid",
  "filename": "toyota.pdf",
  "status": "deleted",
  "deleted_chunks": 125
}
```

**Ví dụ curl**

```powershell
curl -X DELETE http://localhost:9090/ai/api/v1/documents/document-uuid `
  -H "Authorization: Bearer <admin-access-token>"
```

**Lỗi thường gặp**

- `403`: tài khoản không có quyền Admin.
- `404`: không tìm thấy tài liệu.
- `503`: MongoDB hoặc Qdrant không sẵn sàng.

### `POST /api/v1/sessions/{session_id}/reset`

Xóa trạng thái hội thoại hiện tại của một session.

**Chức năng**

- Reset slots, stage và lịch sử hội thoại của `session_id`.
- Dùng khi người dùng muốn bắt đầu cuộc tư vấn mới hoặc frontend cần làm mới session.

**Path param**

| Tên | Mô tả |
| --- | --- |
| `session_id` | ID phiên chat cần reset. |

**Response**

```json
{
  "session_id": "session-1",
  "status": "reset"
}
```

**Ví dụ curl**

```powershell
curl -X POST http://localhost:9090/ai/api/v1/sessions/session-1/reset
```

**Lỗi thường gặp**

- `503`: không reset được session do lỗi MongoDB.

## Luồng xử lý tài liệu

1. Client upload PDF qua `POST /api/v1/documents`.
2. Service lưu file vào MongoDB GridFS và trả về `job_id`.
3. Background job materialize PDF tạm thời ra filesystem.
4. Nội dung PDF được tách trang, chia chunk và tạo embedding.
5. Chunk được upsert vào Qdrant kèm metadata `source`, `page`, `document_id`, `gridfs_file_id`.
6. Client gọi `GET /api/v1/documents/jobs/{job_id}` để theo dõi kết quả.
7. Khi cần gỡ tài liệu khỏi kho tri thức, client gọi `DELETE /api/v1/documents/{document_id}`.

Lệnh ingest từ file có sẵn trong container chỉ nên dùng khi phát triển local:

```powershell
docker compose exec ai-service python rag.py --rebuild --pdf-path /app/documents
```

## Chạy local ngoài Docker

Khởi động MongoDB và Qdrant từ backend root:

```powershell
cd Tayota\tayota-backend
docker compose up -d mongodb qdrant
```

Cấu hình `.env` tối thiểu:

```env
QDRANT_URL=http://localhost:6333
QDRANT_API_KEY=
MONGO_URI=mongodb://tayota:123456@localhost:27017/tayota_ai_db?authSource=admin
MONGO_DB=tayota_ai_db
MONGO_GRIDFS_BUCKET=ai_pdfs
COLLECTION=atbm_httt
STATE_BACKEND=mongo
SESSION_TTL_SECS=3600
LLM_PROVIDER=groq
GROQ_API_KEY=your_groq_api_key
GROQ_MODEL=llama-3.3-70b-versatile
```

Cài dependency và chạy service:

```powershell
cd Tayota\tayota-backend\ai-service
python -m pip install -r requirements.txt
python -m uvicorn app:app --reload --port 8094
```

## Ghi chú tích hợp frontend

- Luôn gửi `X-AI-Session-Id` khi gọi API chat.
- Với người dùng chưa đăng nhập, frontend vẫn có thể tự tạo session ID và bỏ qua `X-User-Id`.
- Nếu gọi qua gateway từ trình duyệt, base URL là `http://localhost:9090/ai`.
- Nếu gọi trực tiếp service khi phát triển local, base URL là `http://localhost:8094`.
