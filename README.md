# Tayota

Tayota là đồ án web quản lý và tư vấn dịch vụ Toyota, gồm frontend Next.js, backend Spring Boot microservices và AI service FastAPI dùng RAG để tư vấn xe dựa trên tài liệu PDF.

## 1. Kiến trúc tổng quan

```text
tayota-frontend (Next.js)
  -> API Gateway :9090
      -> Operation Service :8091
          -> PostgreSQL :5432
      -> AI Service :8094
          -> MongoDB/GridFS :27017
          -> Qdrant :6333
          -> LLM provider (Groq/mặc định)
```

Frontend chỉ nên gọi backend qua API Gateway (`http://localhost:9090`). Gateway định tuyến:

- `/user/**`, `/car/**`, `/operation/**` sang `operation-service`.
- `/ai/**` sang `ai-service`.
- `/user/chat/ws/**` sang WebSocket của live chat trong `operation-service`.

## 2. Cấu trúc thư mục

```text
Tayota/
  tayota-frontend/                 # Ứng dụng Next.js/React
    src/app/                       # App Router pages
    src/components/                # UI components
    src/lib/                       # API client, services, helper
    public/images/                 # Ảnh tĩnh
    package.json
    Dockerfile

  tayota-backend/
    docker-compose.yml             # Chạy gateway, operation, AI và database
    docker-compose.test.yml        # Profile test cho operation-service
    database-data/                 # Script SQL/dữ liệu bổ sung

    api-gateway/                   # Spring Cloud Gateway
      src/main/resources/application.yml
      src/main/java/com/tayota/apigateway/
      pom.xml
      Dockerfile

    operation-service/             # Spring Boot nghiệp vụ chính
      src/main/java/com/tayota/operationservice/
        controller/                # REST/WebSocket controllers
        service/                   # Business logic
        repository/                # JPA repositories
        entity/                    # Entity PostgreSQL
        dto/                       # Request/response DTO
        mapper/                    # Mapping entity -> DTO
        config/                    # Security, cache, websocket, seed data
      src/main/resources/
        schema.sql
        data.sql
        application.properties
      pom.xml
      Dockerfile

    ai-service/                    # FastAPI RAG service
      app.py                       # API routes, middleware, health check
      rag.py                       # Luồng truy xuất và sinh câu trả lời
      vector_database.py           # Qdrant
      mongo_storage.py             # MongoDB/GridFS
      conversation_state_manager.py
      documents/                   # PDF nguồn cho RAG
      tests/
      requirements.txt
      Dockerfile
```

## 3. Công nghệ chính

- Frontend: Next.js 16, React 19, Tailwind CSS 4, Axios, STOMP WebSocket.
- API Gateway: Java 21, Spring Boot 4, Spring Cloud Gateway WebFlux, JWT.
- Operation Service: Java 21, Spring Boot 4, Spring Security, JPA, PostgreSQL, WebSocket, Cloudinary, Mail.
- AI Service: Python 3.11, FastAPI, Qdrant, MongoDB/GridFS, sentence-transformers, Groq.
- Hạ tầng local: Docker Compose, PostgreSQL 17, MongoDB 8, Qdrant 1.7.4.

## 4. Yêu cầu môi trường

Cần cài đặt:

- Docker Desktop hoặc Docker Engine có Docker Compose.
- Node.js 20+ và npm nếu chạy frontend trên máy host.
- Java 21 và Maven Wrapper nếu chạy Spring service riêng lẻ.
- Python 3.11 nếu chạy AI service riêng lẻ.

Khuyến nghị cho local development: chạy backend bằng Docker Compose, frontend bằng `npm run dev`.

## 5. Cấu hình biến môi trường

Backend Compose có giá trị mặc định để chạy local. Tạo file `.env` trong `tayota-backend/` khi cần thay đổi:

```env
API_GATEWAY_PORT=9090
OPERATION_SERVICE_PORT=8091
AI_SERVICE_PORT=8094
FRONTEND_ORIGINS=http://localhost:3000

POSTGRES_USER=tayota
POSTGRES_PASSWORD=123456
POSTGRES_OPERATION_DB=tayota_operation_db

MONGO_USER=tayota
MONGO_PASSWORD=123456
MONGO_AI_DB=tayota_ai_db

JWT_SECRET=change-me-to-a-long-random-secret
GATEWAY_INTERNAL_SECRET=change-me-gateway-internal-secret

LLM_PROVIDER=groq
GROQ_API_KEY=
GROQ_MODEL=llama-3.3-70b-versatile

MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=

CLOUDINARY_CLOUD_NAME=
CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=
CLOUDINARY_FOLDER_PREFIX=tayota
```

Frontend đọc API base từ `NEXT_PUBLIC_API_BASE_URL`. Tạo `tayota-frontend/.env.local` nếu cần:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:9090
```

Nếu không cấu hình email, Cloudinary hoặc Groq key, các tính năng liên quan có thể bị giới hạn, nhưng backend vẫn có thể khởi động với cấu hình mặc định.

## 6. Chạy dự án

### 6.1. Chạy backend bằng Docker Compose

```powershell
cd tayota-backend
docker compose up --build

Load tài liệu AI cơ bản để test: docker compose exec ai-service python vector_database.py --rebuild --pdf-path /app/documents
```

Sau khi khởi động:

- API Gateway: `http://localhost:9090`
- Operation Service nội bộ Compose: `operation-service:8091`
- AI Service: `http://localhost:8094`
- PostgreSQL: `localhost:5432`
- MongoDB: `localhost:27017`
- Qdrant dashboard/API: `http://localhost:6333`

Kiểm tra log:

```powershell
cd tayota-backend
docker compose logs -f api-gateway operation-service ai-service
```

Dừng backend:

```powershell
cd tayota-backend
docker compose down
```

Xóa cả volume dữ liệu local:

```powershell
cd tayota-backend
docker compose down -v
```

### 6.2. Chạy frontend

```powershell
cd tayota-frontend
npm ci
npm run dev
```

Mở trình duyệt tại `http://localhost:3000`.

Build production:

```powershell
cd tayota-frontend
npm run build
npm run start
```

### 6.3. Chạy bằng Docker riêng cho frontend

Frontend có `Dockerfile`, nhưng hiện chưa được gắn vào `tayota-backend/docker-compose.yml`. Nếu muốn build riêng với API mặc định `http://localhost:9090`:

```powershell
cd tayota-frontend
docker build -t tayota-frontend .
docker run --rm -p 3000:3000 tayota-frontend
```

Lưu ý: biến `NEXT_PUBLIC_API_BASE_URL` của Next.js được đóng vào bundle lúc build. Nếu cần đổi API URL cho image Docker production, hãy cấu hình biến này trước bước build hoặc bổ sung `ARG/ENV` tương ứng trong `Dockerfile`.

## 7. Tài khoản demo

`operation-service` tự động seed các tài khoản demo khi khởi động. Mật khẩu mặc định:

```text
Tayota@123
```

Tài khoản:

- `admin.demo@tayota.com` - ADMIN
- `manager.demo@tayota.com` - MANAGER
- `advisor.demo@tayota.com` - SERVICE_ADVISOR
- `assistant.demo@tayota.com` - ASSISTANT
- `mechanic.demo@tayota.com` - MECHANIC
- `customer.demo@tayota.com` - USER

Có thể đổi mật khẩu seed bằng biến `tayota.seed.admin-password` khi chạy service riêng, hoặc thêm mapping env tương ứng trong Compose nếu cần.

## 8. Kiểm thử

### Operation Service

Chạy test trực tiếp:

```powershell
cd tayota-backend/operation-service
./mvnw test
```

Hoặc chạy qua Docker Compose profile test:

```powershell
cd tayota-backend
docker compose -f docker-compose.test.yml --profile test up --build --abort-on-container-exit
```

### API Gateway

```powershell
cd tayota-backend/api-gateway
./mvnw test
```

### AI Service

Nếu chạy trên host:

```powershell
cd tayota-backend/ai-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
pytest
```

### Frontend

```powershell
cd tayota-frontend
npm run lint
npm run build
```

## 9. API và module nghiệp vụ

Operation Service quản lý các nhóm chính:

- Xác thực, refresh token, quản lý thiết bị: `/user/**`
- Catalog xe, dòng xe, phiên bản, thông số, giá, hình ảnh: `/car/**`
- Lịch hẹn lái thử và bảo dưỡng: `/operation/appointments/**`
- Work order, cố vấn dịch vụ, kỹ thuật viên: `/operation/workorders/**`
- Live chat khách hàng/nhân viên: `/user/chat/**`, `/user/chat/ws/**`
- Review, notification, media upload, admin/manager dashboard.

AI Service quản lý:

- Chat RAG: `/ai/api/v1/chat`
- Lịch sử session AI: `/ai/api/v1/users/{user_id}/sessions`, `/ai/api/v1/sessions/{session_id}/messages`
- Quản lý tài liệu PDF cho admin: `/ai/api/v1/documents/**`
- Health check qua gateway: `/ai/health`

## 10. Ghi chú phát triển

- Không gọi trực tiếp `operation-service` từ frontend; dùng Gateway để JWT/header nội bộ được xử lý đúng.
- AI service kiểm tra `X-Gateway-Secret` cho `/health` và `/api/v1/**`; frontend không được tự gắn secret này.
- Dữ liệu PostgreSQL local được lưu ở `tayota-backend/docker-data/postgres-operation-service`.
- Dữ liệu MongoDB và Qdrant local được lưu ở `tayota-backend/docker-data/mongo-data` và `tayota-backend/docker-data/qdrant-storage`.
- `schema.sql` và `data.sql` trong `operation-service` được chạy khi Compose cấu hình `SPRING_SQL_INIT_MODE=always`.
- Trước khi commit nên chạy tối thiểu: backend test liên quan, `npm run lint`, và `npm run build` nếu có thay đổi frontend.
