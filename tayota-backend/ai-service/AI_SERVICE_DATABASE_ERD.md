# AI Service Database cho ERD

Tài liệu này mô tả cấu trúc lưu trữ hiện tại của `tayota-backend/ai-service` để vẽ ERD/logical data model. Source chính đã đối chiếu gồm `app.py`, `mongo_storage.py`, `conversation_state_manager.py`, `vector_database.py`, `chunking.py` và `data_processing/extract_pdf.py`.

## Tổng quan storage

AI service dùng hai hệ lưu trữ chính:

| Storage | Tên mặc định | Vai trò |
| --- | --- | --- |
| MongoDB | `tayota_ai_db` | Metadata PDF, trạng thái job ingest, session hội thoại và lịch sử chat. |
| GridFS trong MongoDB | bucket `ai_pdfs` | Nội dung binary của file PDF upload. Tạo 2 collection vật lý: `ai_pdfs.files`, `ai_pdfs.chunks`. |
| Qdrant | collection `atbm_httt` | Vector embedding và payload của từng chunk tài liệu phục vụ RAG. |

Các tên trên có thể đổi bằng biến môi trường:

| Biến môi trường | Giá trị mặc định |
| --- | --- |
| `MONGO_DB` / `MONGO_AI_DB` | `tayota_ai_db` |
| `MONGO_GRIDFS_BUCKET` | `ai_pdfs` |
| `COLLECTION` | `atbm_httt` |
| `SESSION_TTL_SECS` | `3600` |

## Các bảng nên vẽ ERD cho báo cáo

Nếu vẽ ERD ở mức báo cáo/logic nghiệp vụ, nên vẽ các bảng/entity sau. Các tên này đã giản lược để dễ trình bày, dù hệ thống thật dùng MongoDB, GridFS và Qdrant.

| STT | Bảng/entity nên vẽ | Nguồn dữ liệu thật | Vai trò trong hệ thống | Ghi chú khi vẽ |
| --- | --- | --- | --- | --- |
| 1 | `User` | Operation Service | Người dùng hoặc admin tương tác với AI service. | Không nằm trong AI database; chỉ vẽ để thể hiện khóa `user_id`. |
| 2 | `AI_Session` | MongoDB `ai_sessions` | Lưu trạng thái phiên hội thoại AI. | Khóa chính logic là `session_id`. |
| 3 | `AI_Chat_Message` | MongoDB `ai_chat_messages` | Lưu từng lượt hỏi đáp giữa người dùng và AI. | Liên kết với `AI_Session` qua `session_id`. |
| 4 | `AI_Document` | MongoDB `ai_documents` | Lưu metadata PDF do admin upload. | Nội dung file thật nằm trong GridFS, field nối là `gridfs_file_id`. |
| 5 | `AI_Document_Job` | MongoDB `ai_document_jobs` | Theo dõi job ingest/indexing tài liệu PDF. | Liên kết với `AI_Document` qua `document_id`. |
| 6 | `Document_Chunk` | Qdrant collection `atbm_httt` | Đại diện các chunk/vector được tạo từ tài liệu. | Không cần vẽ vector embedding chi tiết; chỉ ghi chú lưu trong Qdrant. |
| 7 | `Chat_Message_Source` | Mảng `sources[]` trong `ai_chat_messages` | Lưu nguồn/chunk đã được dùng để trả lời một message. | Đây là bảng logic để ERD dễ đọc, không phải collection riêng. |

Quan hệ chính nên thể hiện:

```text
User 1 - N AI_Session
User 1 - N AI_Document

AI_Session 1 - N AI_Chat_Message

AI_Document 1 - N AI_Document_Job
AI_Document 1 - N Document_Chunk

AI_Chat_Message 1 - N Chat_Message_Source
Document_Chunk 1 - N Chat_Message_Source
```

Không bắt buộc vẽ `GRIDFS_FILE` và `GRIDFS_CHUNK` trong ERD báo cáo nếu chỉ cần mô hình nghiệp vụ. Chỉ vẽ thêm hai entity này khi cần mô tả storage vật lý của MongoDB GridFS.

## Sơ đồ quan hệ logic

```mermaid
erDiagram
    OPERATION_USER ||--o{ AI_SESSION : "external user_id"
    OPERATION_USER ||--o{ AI_DOCUMENT : "uploaded_by_user_id"

    AI_SESSION ||--o{ AI_CHAT_MESSAGE : "session_id"
    AI_DOCUMENT ||--o{ AI_DOCUMENT_JOB : "document_id"
    AI_DOCUMENT ||--|| GRIDFS_FILE : "gridfs_file_id"
    GRIDFS_FILE ||--o{ GRIDFS_CHUNK : "files_id"
    AI_DOCUMENT ||--o{ QDRANT_POINT : "payload.document_id"
    QDRANT_POINT ||--o{ AI_CHAT_MESSAGE_SOURCE : "chunk_id/document_id snapshot"
    AI_CHAT_MESSAGE ||--o{ AI_CHAT_MESSAGE_SOURCE : "sources[]"

    OPERATION_USER {
        string user_id "Ngoài AI DB, thuộc operation-service"
    }

    AI_SESSION {
        string _id PK "Bằng session_id"
        string session_id
        string user_id FK_nullable "External user id"
        object slots
        array recent_history
        array intent_history
        string stage
        string last_intent
        int turn_count
        float created_at
        float updated_at
        datetime created_at_iso
        datetime updated_at_iso
        string status
    }

    AI_CHAT_MESSAGE {
        objectId _id PK
        string session_id FK
        string user_id FK_nullable "External user id"
        string question
        string answer
        string intent
        string stage
        object slots_snapshot
        array sources
        string model_used
        string rule_triggered
        datetime created_at
    }

    AI_CHAT_MESSAGE_SOURCE {
        string source
        int page
        float score
        string chunk_id
        int chunk_index
        string document_id FK_nullable
        string gridfs_file_id FK_nullable
    }

    AI_DOCUMENT {
        objectId _id PK
        string document_id UK
        objectId gridfs_file_id FK
        string filename
        string content_type
        int size_bytes
        string sha256
        datetime uploaded_at
        string uploaded_by_user_id FK_nullable "External user id"
        string status
        datetime updated_at
    }

    AI_DOCUMENT_JOB {
        objectId _id PK
        string job_id UK
        string status
        string message
        string document_id FK_nullable
        int indexed_pages
        int indexed_chunks
        datetime created_at
        datetime updated_at
    }

    GRIDFS_FILE {
        objectId _id PK
        int length
        int chunkSize
        datetime uploadDate
        string filename
        string contentType
        object metadata
    }

    GRIDFS_CHUNK {
        objectId _id PK
        objectId files_id FK
        int n
        binary data
    }

    QDRANT_POINT {
        uuid id PK
        vector embedding
        string chunk_id UK
        string content
        string source
        string source_id
        string source_path
        int page
        int total_pages
        int chunk_index
        int char_start
        int char_end
        string content_hash
        string document_id FK_nullable
        string gridfs_file_id FK_nullable
    }
```

Ghi chú khi vẽ ERD:

- `OPERATION_USER` không nằm trong AI database. `user_id` là id người dùng do gateway/operation-service truyền sang qua trusted header.
- MongoDB và Qdrant không enforce foreign key. Các quan hệ trên là quan hệ logic theo field được code dùng để truy vấn/xóa đồng bộ.
- `AI_CHAT_MESSAGE_SOURCE` không phải collection riêng; đây là phần tử trong mảng `ai_chat_messages.sources[]`. Tách ra trong ERD giúp nhìn quan hệ đến Qdrant/document rõ hơn.
- `QDRANT_POINT` không phải Mongo collection; đây là point trong Qdrant collection `atbm_httt`.

## MongoDB collections

### `ai_documents`

Lưu metadata của file PDF admin upload. Nội dung PDF thật nằm trong GridFS.

| Field | Kiểu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| `_id` | `ObjectId` | Có | Primary key MongoDB tự sinh. |
| `document_id` | `string` | Có | UUID do app sinh, dùng làm khóa nghiệp vụ. |
| `gridfs_file_id` | `ObjectId` | Có | Trỏ đến `ai_pdfs.files._id`. |
| `filename` | `string` | Có | Tên file PDF đã chuẩn hóa bằng `Path(filename).name`. |
| `document_category` | `string/null` | Không | Nhóm tài liệu ổn định dùng để filter RAG; nếu không truyền thì service tự suy ra từ filename/content. |
| `content_type` | `string` | Có | Thường là `application/pdf`. |
| `size_bytes` | `int` | Có | Kích thước file upload. |
| `sha256` | `string` | Có | Hash SHA-256 của nội dung PDF. |
| `uploaded_at` | `datetime` | Có | Thời điểm upload. |
| `uploaded_by_user_id` | `string/null` | Không | User id admin từ `X-User-Id`. |
| `status` | `string` | Có | `uploaded`, `indexing`, `indexed`, `failed`. |
| `updated_at` | `datetime` | Không | Được set khi đổi trạng thái. |

Luồng trạng thái thường gặp:

```text
uploaded -> indexing -> indexed
uploaded -> indexing -> failed
```

### `ai_document_jobs`

Lưu trạng thái job ingest/indexing PDF để frontend theo dõi.

| Field | Kiểu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| `_id` | `ObjectId` | Có | Primary key MongoDB tự sinh. |
| `job_id` | `string` | Có | UUID job, dùng để tra `/api/v1/documents/jobs/{job_id}`. |
| `status` | `string` | Có | `queued`, `running`, `success`, `failed`. |
| `message` | `string` | Có | Mô tả trạng thái/lỗi. |
| `document_id` | `string/null` | Không | Tài liệu kích hoạt job. |
| `indexed_pages` | `int` | Có | Hiện model API có field này, mặc định `0`. |
| `indexed_chunks` | `int` | Có | Số vector/chunk sau ingest theo kết quả Qdrant count. |
| `created_at` | `datetime` | Có | Set khi insert lần đầu. |
| `updated_at` | `datetime` | Có | Set mỗi lần upsert trạng thái job. |

Quan hệ: nhiều job có thể cùng trỏ đến một `ai_documents.document_id` nếu tài liệu được upload/index lại.

### `ai_sessions`

Lưu state hội thoại AI. Collection này được dùng khi `STATE_BACKEND=mongo`, là mặc định.

| Field | Kiểu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| `_id` | `string` | Có | Bằng `session_id`; dùng làm primary key logic. |
| `session_id` | `string` | Có | Id phiên AI, thường nhận từ `X-AI-Session-Id`. |
| `user_id` | `string/null` | Không | User id đăng nhập; khách vãng lai có thể `null`. |
| `slots` | `object` | Có | Nhu cầu đã trích xuất: `budget`, `seats`, `purpose`, ... tùy `slot_extractor.empty_slots()`. |
| `recent_history` | `array<object>` | Có | Lịch sử gần đây, mỗi item dạng `{role, content}`. Tối đa 10 lượt user/assistant. |
| `intent_history` | `array<string>` | Có | Danh sách intent đã nhận diện. |
| `stage` | `string` | Có | `greeting`, `collecting`, `advising`, `done`. |
| `last_intent` | `string/null` | Không | Intent gần nhất. |
| `turn_count` | `int` | Có | Số lượt hỏi đáp đã ghi vào state. |
| `created_at` | `float` | Có | Unix timestamp dạng `time.time()`. |
| `updated_at` | `float` | Có | Unix timestamp dạng `time.time()`. |
| `created_at_iso` | `datetime` | Có | Bản datetime để sort/hiển thị. |
| `updated_at_iso` | `datetime` | Có | Bản datetime để sort/hiển thị. |
| `status` | `string` | Có | Hiện mặc định `active`. |

Quan hệ:

- `ai_sessions.session_id` liên kết với `ai_chat_messages.session_id`.
- `ai_sessions.user_id` là khóa ngoài logic đến user của operation-service.

### `ai_chat_messages`

Lưu từng lượt chat hoàn chỉnh sau khi pipeline trả lời xong.

| Field | Kiểu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| `_id` | `ObjectId` | Có | Primary key MongoDB tự sinh. |
| `session_id` | `string` | Có | Liên kết logic đến `ai_sessions.session_id`. |
| `user_id` | `string/null` | Không | User id từ request hoặc từ session state. |
| `question` | `string` | Có | Câu hỏi người dùng. |
| `answer` | `string` | Có | Câu trả lời cuối cùng. |
| `intent` | `string` | Có | Intent do classifier xác định. |
| `stage` | `string` | Có | Stage của session tại thời điểm trả lời. |
| `slots_snapshot` | `object` | Có | Snapshot các slot đã có giá trị. |
| `sources` | `array<object>` | Có | Danh sách nguồn RAG đã dùng. Có thể rỗng nếu rule chặn hoặc không retrieve. |
| `model_used` | `string` | Có | Ví dụ `llama-3.3-70b-versatile`, `business_rules`, `none`. |
| `rule_triggered` | `string` | Có | Tên rule đã kích hoạt, hoặc chuỗi rỗng. |
| `created_at` | `datetime` | Có | Thời điểm ghi log. |

Schema phần tử trong `sources[]`:

| Field | Kiểu | Mô tả |
| --- | --- | --- |
| `source` | `string/null` | Tên PDF/source trong Qdrant payload. |
| `document_category` | `string/null` | Nhóm tài liệu ổn định dùng để lọc RAG, ví dụ `basic_advice`, `summary`, `suv`, `sedan`. |
| `page` | `int/null` | Trang PDF. |
| `score` | `float/null` | Điểm truy xuất sau search/rerank. |
| `chunk_id` | `string/null` | Khóa chunk trong Qdrant payload. |
| `chunk_index` | `int/null` | Thứ tự chunk trong trang. |
| `document_id` | `string/null` | Trỏ logic đến `ai_documents.document_id` nếu chunk đến từ PDF upload qua Mongo/GridFS. |
| `gridfs_file_id` | `string/null` | Id file GridFS dạng string snapshot từ Qdrant payload. |

## GridFS bucket `ai_pdfs`

GridFS tự tạo 2 collection vật lý theo bucket name.

### `ai_pdfs.files`

Lưu metadata file binary.

| Field | Kiểu | Mô tả |
| --- | --- | --- |
| `_id` | `ObjectId` | Primary key file GridFS, được lưu lại ở `ai_documents.gridfs_file_id`. |
| `length` | `int` | Tổng kích thước file. |
| `chunkSize` | `int` | Kích thước mỗi chunk GridFS. |
| `uploadDate` | `datetime` | Thời điểm GridFS ghi file. |
| `filename` | `string` | Tên file PDF. |
| `contentType` | `string` | Content type do `bucket.put(..., content_type=...)` ghi. |
| `metadata.document_id` | `string` | Trùng `ai_documents.document_id`. |
| `metadata.uploaded_by_user_id` | `string/null` | User id admin upload. |
| `metadata.sha256` | `string` | Hash SHA-256 của PDF. |
| `metadata.document_category` | `string/null` | Snapshot category tài liệu nếu có khi upload. |

### `ai_pdfs.chunks`

Lưu các mảnh binary của file.

| Field | Kiểu | Mô tả |
| --- | --- | --- |
| `_id` | `ObjectId` | Primary key chunk GridFS. |
| `files_id` | `ObjectId` | Trỏ đến `ai_pdfs.files._id`. |
| `n` | `int` | Số thứ tự chunk trong file. |
| `data` | `binary` | Nội dung binary. |

## Qdrant collection `atbm_httt`

Qdrant lưu point vector, không nằm trong MongoDB. Collection được tạo với:

| Thuộc tính | Giá trị |
| --- | --- |
| Vector size | `384` |
| Distance | `Cosine` |
| Point id | UUID v5 sinh từ `chunk_id` |

Payload của mỗi point:

| Field | Kiểu | Mô tả |
| --- | --- | --- |
| `chunk_id` | `string` | Id ổn định của chunk, sinh từ source/page/index/char/hash. |
| `content` | `string` | Nội dung text chunk. |
| `source` | `string/null` | Tên tài liệu, thường là filename PDF hoặc `summary`. |
| `source_id` | `string/null` | Id nguồn ổn định, ví dụ `mongo-{document_id}` khi ingest từ GridFS. |
| `source_path` | `string/null` | Đường dẫn nguồn; với GridFS là `gridfs://{gridfs_file_id}`. |
| `document_category` | `string/null` | Nhóm tài liệu ổn định để lọc retrieval thay vì phụ thuộc exact filename. |
| `page` | `int/null` | Trang PDF. |
| `total_pages` | `int/null` | Tổng số trang của PDF. |
| `chunk_index` | `int/null` | Thứ tự chunk trong trang. |
| `char_start` | `int/null` | Vị trí ký tự bắt đầu trong text trang. |
| `char_end` | `int/null` | Vị trí ký tự kết thúc trong text trang. |
| `content_hash` | `string/null` | SHA-256 rút gọn 12 ký tự của nội dung chunk. |
| `document_id` | `string/null` | Trỏ logic về `ai_documents.document_id`. |
| `gridfs_file_id` | `string/null` | Trỏ logic về `ai_pdfs.files._id` dạng string. |

Point đặc biệt `summary_all_cars`:

- `source = "summary"`
- `source_id = "summary"`
- `document_category = "summary"`
- `page = 0`
- `chunk_index = -1`
- Thường không có `document_id` và `gridfs_file_id`.

## Luồng dữ liệu chính

### Upload và ingest PDF

1. Admin gọi `POST /api/v1/documents` qua gateway.
2. AI service lưu binary PDF vào GridFS bucket `ai_pdfs`.
3. AI service lưu metadata vào `ai_documents` với `status = "uploaded"`.
4. AI service tạo bản ghi `ai_document_jobs` với `status = "queued"`.
5. Background job đổi document sang `indexing`.
6. PDF được materialize tạm từ GridFS, tách trang, chia chunk, embed và upsert vào Qdrant.
7. Nếu thành công, document sang `indexed`, job sang `success`; nếu lỗi, document sang `failed`, job sang `failed`.

### Chat RAG

1. Gateway truyền `X-AI-Session-Id` và có thể truyền `X-User-Id`.
2. AI service lấy hoặc tạo `ai_sessions` theo `session_id`.
3. Pipeline phân loại intent, trích slot, retrieve Qdrant và gọi LLM khi cần.
4. State mới được ghi lại vào `ai_sessions`.
5. Một log đầy đủ được insert vào `ai_chat_messages`, kèm snapshot `sources[]` từ Qdrant.

### Xóa tài liệu

1. Admin gọi `DELETE /api/v1/documents/{document_id}`.
2. Service xóa các point Qdrant có `payload.document_id = document_id`.
3. Service xóa file GridFS theo `ai_documents.gridfs_file_id`.
4. Service xóa metadata trong `ai_documents`.

## Gợi ý index/unique constraint khi triển khai thực tế

Code hiện tại chưa khai báo index MongoDB trong source. Khi thiết kế ERD hoặc hardening database, nên thể hiện các index logic sau:

| Collection/storage | Field | Loại đề xuất | Lý do |
| --- | --- | --- | --- |
| `ai_documents` | `document_id` | Unique | Tra cứu/xóa theo document id. |
| `ai_documents` | `status`, `uploaded_at` | Non-unique | List tài liệu theo trạng thái, sort mới nhất. |
| `ai_document_jobs` | `job_id` | Unique | Tra cứu job theo API. |
| `ai_document_jobs` | `document_id` | Non-unique | Xem các job của một tài liệu. |
| `ai_sessions` | `_id` | Primary/unique | Lấy session theo id. |
| `ai_sessions` | `user_id`, `updated_at` | Non-unique compound | List session của user theo mới nhất. |
| `ai_sessions` | `status` | Non-unique | Đếm session active. |
| `ai_chat_messages` | `session_id`, `created_at` | Non-unique compound | List messages theo session theo thứ tự thời gian. |
| `ai_pdfs.chunks` | `files_id`, `n` | GridFS standard | Đọc file theo thứ tự chunk. |
| Qdrant `atbm_httt` | `document_id` payload index | Payload index | Xóa/lọc chunk theo tài liệu. |
| Qdrant `atbm_httt` | `document_category`, `source`, `source_id`, `page`, `chunk_index` payload index | Payload index | Search theo nhóm tài liệu ổn định, fallback source cũ và lấy neighbor chunks. |

## Những điểm không nên vẽ như bảng chính

- `.processed_pdfs.json`: cache runtime trên file system để bỏ qua PDF không đổi trong ingest incremental, không phải database nguồn sự thật.
- `documents/*.pdf`: bộ PDF seed/local, không phải bảng runtime.
- Cache hệ thống: AI service hiện trả `system_cache = "unused"` trong health response và không dùng cache hệ thống để lưu state.
