# Tayota — Nền tảng quản lý dịch vụ và tư vấn Toyota

> Ngôn ngữ: [English](README.md) | **Tiếng Việt**

<div align="center">

![Next.js](https://img.shields.io/badge/Next.js-16.2.6-000000?style=flat-square&logo=next.js&logoColor=white)
![React](https://img.shields.io/badge/React-19.2.4-61DAFB?style=flat-square&logo=react&logoColor=white)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-4-38BDF8?style=flat-square&logo=tailwind-css&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.5-6DB33F?style=flat-square&logo=spring-boot&logoColor=white)
![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Cloud](https://img.shields.io/badge/Spring_Cloud_Gateway-2025.1.1-6DB33F?style=flat-square&logo=spring&logoColor=white)
![FastAPI](https://img.shields.io/badge/FastAPI-0.104.1-009688?style=flat-square&logo=fastapi&logoColor=white)
![Python](https://img.shields.io/badge/Python-3.11-3776AB?style=flat-square&logo=python&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-8-47A248?style=flat-square&logo=mongodb&logoColor=white)
![Qdrant](https://img.shields.io/badge/Qdrant-1.7.4-000000?style=flat-square)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=flat-square&logo=docker&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)

**Tayota** là nền tảng microservices phục vụ quản lý dịch vụ và tư vấn Toyota, gồm frontend Next.js, backend Spring Boot và service AI FastAPI dùng RAG để tư vấn xe từ tài liệu PDF.

[Tính năng](#tính-năng) · [Kiến trúc](#kiến-trúc) · [Cài đặt](#cài-đặt) · [Tài khoản demo](#tài-khoản-demo) · [Tài liệu API](#tài-liệu-api) · [Kiểm thử](#kiểm-thử) · [Triển khai](#triển-khai)

</div>

---

## Mục lục

- [Tính năng](#tính-năng)
- [Kiến trúc](#kiến-trúc)
  - [Tổng quan hệ thống](#tổng-quan-hệ-thống)
  - [Luồng xử lý chính](#luồng-xử-lý-chính)
  - [Database Schema](#database-schema)
  - [Chiến lược render Frontend](#chiến-lược-render-frontend)
  - [Cấu trúc dự án](#cấu-trúc-dự-án)
- [Tech Stack](#tech-stack)
- [Yêu cầu](#yêu-cầu)
- [Cài đặt](#cài-đặt)
- [Tài khoản demo](#tài-khoản-demo)
- [Tài liệu API](#tài-liệu-api)
- [Kiểm thử](#kiểm-thử)
- [Triển khai](#triển-khai)
- [License](#license)

---

## Tính năng

### Khách hàng (USER)
- Đăng ký và đăng nhập bằng email/mật khẩu (JWT + Refresh Token qua HTTP-only cookie).
- Đăng nhập Google OAuth 2.0.
- Xác minh email, quên/đổi mật khẩu và quản lý phiên đăng nhập.
- Xem catalog xe Toyota theo dòng xe, phiên bản, thông số và giá.
- Đặt lịch lái thử và lịch bảo dưỡng với giới hạn theo ngày và thời gian báo trước tối thiểu.
- Chat trực tiếp với advisor qua WebSocket (STOMP).
- Tư vấn xe bằng AI RAG từ tài liệu PDF nội bộ.
- Nhận email thông báo khi lịch hẹn thay đổi.
- Gửi đánh giá sau khi hoàn tất dịch vụ qua token tự hết hạn.
- Xem thông tin tài khoản và lịch sử dịch vụ.

### Service Advisor
- Tiếp nhận và quản lý lịch lái thử/bảo dưỡng.
- Tạo work order và phân công mechanic.
- Cập nhật trạng thái work order và gửi thông báo cho khách hàng.
- Hỗ trợ khách hàng qua live chat realtime.
- Dashboard cá nhân cho lịch hẹn, work order và thống kê.

### Assistant
- Quản lý catalog xe và thông tin model.
- Quản lý thông số, giá và hình ảnh xe.
- Hỗ trợ advisor xử lý yêu cầu của khách hàng.

### Mechanic
- Nhận và cập nhật trạng thái work order được phân công.
- Báo cáo tiến độ sửa chữa qua dashboard.
- Gửi thông báo khi hoàn tất công việc.

### Manager
- Dashboard tổng quan hệ thống: lịch hẹn, work order, doanh thu.
- Quản lý nhân sự và phân quyền.
- Quản lý toàn bộ catalog xe.
- Thống kê và báo cáo chi tiết theo thời gian.

### Admin
- Quản lý tài khoản: xem, khóa/mở khóa, đổi vai trò.
- Dashboard tổng quan hệ thống.
- Quản lý tài liệu AI: upload PDF, rebuild vector index.
- Xem lịch sử sử dụng AI: phiên chat, token usage.

### Hệ thống
- JWT + Refresh Token với khả năng thu hồi theo từng phiên.
- RBAC với 6 vai trò: `ADMIN`, `MANAGER`, `SERVICE_ADVISOR`, `ASSISTANT`, `MECHANIC`, `USER`.
- API Gateway định tuyến thông minh: `/user/**`, `/car/**`, `/operation/**` -> operation-service; `/ai/**` -> ai-service.
- Docker Compose cho toàn bộ hạ tầng: PostgreSQL, MongoDB, Qdrant, Gateway và các service.
- Cache in-memory bằng Caffeine cho operation-service.
- Realtime qua STOMP WebSocket (live chat) và Server-Sent Events.
- Gateway giao tiếp với service bằng HTTP và WebSocket theo `application.yml`.
- Gửi email qua Spring Mail + SMTP.
- Upload media lên Cloudinary.
- Chatbot RAG với Qdrant vector search + Groq LLM (Llama 3.3).
- Unit test và integration test cho operation-service và ai-service.

---

## Kiến trúc

### Tổng quan hệ thống

Dự án được tách thành bốn tiến trình độc lập: `tayota-frontend` (Next.js), `api-gateway` (Spring Cloud Gateway WebFlux), `operation-service` (Spring Boot 4) và `ai-service` (FastAPI + RAG). Tất cả API từ frontend đi qua Gateway ở port `:9090`.

```mermaid
flowchart TB
    subgraph CLIENTS["Clients"]
        Browser["Browser"]
        Mobile["Mobile"]
    end

    subgraph FRONTEND["Frontend :3000"]
        FE["Next.js 16.2.6\nReact 19 - Tailwind CSS 4\nAxios - STOMP WebSocket"]
    end

    subgraph GATEWAY["API Gateway :9090"]
        GW["Spring Cloud Gateway 2025.1.1\nJava 21 - WebFlux - JWT Filter"]
    end

    subgraph SERVICES["Microservices"]
        OPS["operation-service\nSpring Boot 4.0.5 - :9091"]
        AIS["ai-service\nFastAPI 0.104.1 - :9094"]
    end

    subgraph DATA["Data Layer"]
        PG[("PostgreSQL 17\n:5432")]
        MG[("MongoDB 8\n:27017")]
        QD[("Qdrant 1.7.4\n:6333")]
    end

    subgraph EXTERNAL["External Services"]
        GROQ["Groq API\nLlama 3.3 70B"]
        CLD["Cloudinary\nMedia Upload"]
        SMTP["SMTP\nEmail"]
        GOAUTH["Google OAuth 2.0"]
    end

    CLIENTS -->|HTTPS| FRONTEND
    FRONTEND -->|"/user/** /car/** /operation/**"| GATEWAY
    FRONTEND -->|"/ai/**"| GATEWAY
    FRONTEND -->|"WSS /user/chat/ws - STOMP"| GATEWAY

    GATEWAY -->|"HTTP /user/** /car/** /operation/**"| OPS
    GATEWAY -->|"WebSocket /user/chat/ws"| OPS
    GATEWAY -->|"HTTP /ai/**"| AIS

    OPS -->|JDBC| PG
    OPS -->|SMTP| SMTP
    OPS -->|REST| CLD
    OPS -.->|OAuth 2.0| GOAUTH

    AIS -->|GridFS| MG
    AIS -->|"Vector Search"| QD
    AIS -->|"LLM API"| GROQ
```

**Tóm tắt giao tiếp:**

| Từ | Đến | Giao thức |
|------|----|----------|
| Browser / Mobile | Frontend | HTTPS |
| Frontend | API Gateway | HTTPS / WSS (STOMP) |
| API Gateway | operation-service | HTTP / WebSocket |
| API Gateway | ai-service | HTTP |
| operation-service | PostgreSQL | JDBC |
| operation-service | Cloudinary | REST |
| operation-service | SMTP | SMTP |
| operation-service | Google | OAuth 2.0 |
| ai-service | MongoDB | GridFS |
| ai-service | Qdrant | REST |
| ai-service | Groq | REST |

---

### Luồng xử lý chính

```mermaid
sequenceDiagram
    autonumber
    participant User as Người dùng
    participant FE as tayota-frontend
    participant GW as api-gateway
    participant OPS as operation-service
    participant PG as PostgreSQL
    participant AIS as ai-service
    participant QD as Qdrant
    participant MG as MongoDB/GridFS
    participant LLM as Groq

    User->>FE: Đăng nhập / xem catalog / đặt lịch / quản lý work order
    FE->>GW: REST /user/**, /car/**, /operation/**
    GW->>OPS: StripPrefix + JWT/CORS filtering
    OPS->>PG: Đọc/ghi dữ liệu nghiệp vụ
    PG-->>OPS: Kết quả truy vấn
    OPS-->>GW: API response
    GW-->>FE: JSON response
    FE-->>User: Render giao diện mới

    User->>FE: Bắt đầu live chat
    FE->>GW: STOMP /user/chat/ws
    GW->>OPS: WebSocket route đến operation-service
    OPS-->>FE: Tin nhắn realtime
    FE-->>User: Hiển thị tin nhắn advisor/customer

    User->>FE: Hỏi AI tư vấn xe
    FE->>GW: REST /ai/**
    GW->>AIS: Forward AI request
    AIS->>QD: Truy xuất vector context
    AIS->>MG: Đọc tài liệu/session data
    AIS->>OPS: Lấy catalog context khi cần
    AIS->>LLM: Sinh câu trả lời với context đã truy xuất
    LLM-->>AIS: LLM response
    AIS-->>GW: Chat response
    GW-->>FE: JSON response
    FE-->>User: Hiển thị câu trả lời AI
```

---

### Database Schema

#### PostgreSQL — operation-service

Schema quan hệ đầy đủ cho authentication, catalog xe, lịch hẹn, work order, review, live chat và notification.

```mermaid
erDiagram
    USER {
        uuid id PK
        string email UK
        string password_hash
        string login_provider
        string provider_user_id
        string role
        string status
        timestamp created_at
    }

    CAR_STYLE {
        uuid id PK
        string name
        text description
    }

    CAR_SERIES {
        uuid id PK
        uuid car_style_id FK
        string name
        text description
        timestamp created_at
    }

    CAR_VERSION {
        uuid id PK
        uuid car_series_id FK
        string name
        int sale_percent
        int model_year
        string image_url
        string video_url
        boolean is_visible
        timestamp created_at
    }

    CAR_SPECIFICATION {
        uuid id PK
        uuid car_version_id FK
        string engine
        string transmission
        string fuel_type
        int horsepower
        string dimensions
        decimal fuel_consumption
    }

    CAR_GALLERY {
        uuid id PK
        uuid car_version_id FK
        string image_url
        string caption
    }

    EXTERIOR_COLOR {
        uuid id PK
        string name
        string code
        string image_url
    }

    INTERIOR_COLOR {
        uuid id PK
        string name
        string code
    }

    CAR_PRICE {
        uuid car_version_id FK
        uuid exterior_color_id FK
        uuid interior_color_id FK
        decimal price
        string ex_image_url
        string in_image_url
    }

    DEALERSHIP {
        uuid id PK
        string name
        string address
        string phone
        float latitude
        float longitude
        string operating_hours
        boolean is_active
        timestamp created_at
    }

    SERVICE_TIME_SLOT {
        uuid id PK
        uuid dealership_id FK
        time start_time
        time end_time
        int capacity
        boolean is_active
    }

    CAR {
        string vin_id PK
        uuid car_version_id FK
        uuid dealership_id FK
        string engine_number
        uuid owner_user_id FK
        string status
        int produced_year
        timestamp created_at
    }

    GUEST_INFORMATION {
        uuid id PK
        string full_name
        string email
        string phone
    }

    APPOINTMENT {
        uuid id PK
        uuid user_id FK
        uuid car_version_id FK
        string vin_id FK
        uuid dealership_id FK
        uuid mechanic_id FK
        uuid guest_information_id FK
        string type
        string status
        timestamp scheduled_start_at
        timestamp scheduled_end_at
        text notes
        timestamp confirmed_at
        timestamp completed_at
        timestamp canceled_at
        string cancel_reason
        timestamp created_at
        timestamp updated_at
    }

    SERVICE {
        uuid id PK
        uuid user_id FK
        uuid guest_information_id FK
        string vin_id FK
        uuid mechanic_id FK
        uuid dealership_id FK
        uuid appointment_id FK
        int mileage_at_service
        string status
        decimal total_amount
        string vehicle_condition
        text notes
        timestamp receiving_at
        timestamp processing_at
        timestamp completed_at
        timestamp canceled_at
        string cancel_reason
        timestamp created_at
        timestamp updated_at
    }

    ACCESSORY {
        uuid id PK
        string model
        string brand
        decimal price
        text description
        text use_content
        text reminder_content
        string type
        string image_url
        boolean is_visible
    }

    SERVICE_ITEM {
        uuid id PK
        uuid service_id FK
        string item_type
        uuid accessory_id FK
        string item_name
        int quantity
        decimal unit_price
        string billing_type
        decimal final_price
        text note
        timestamp created_at
    }

    CUSTOMER_REVIEW {
        uuid id PK
        string review_type
        string status
        string review_token
        timestamp token_expires_at
        timestamp submitted_at
        uuid appointment_id FK
        uuid service_id FK
        uuid user_id FK
        string guest_full_name
        string guest_email
        string guest_phone
        uuid dealership_id FK
        int service_rating
        text service_comment
        uuid mechanic_id FK
        int mechanic_rating
        text mechanic_comment
        timestamp created_at
    }

    CHAT_SESSION {
        uuid id PK
        uuid user_id FK
        string guest_id
        uuid assigned_assistant_id FK
        string status
        timestamp closed_at
        timestamp resolved_at
        timestamp created_at
        timestamp updated_at
    }

    CHAT_MESSAGE {
        uuid id PK
        uuid chat_session_id FK
        uuid sender_id FK
        string sender_type
        string message_type
        text content
        timestamp sent_at
    }

    NOTIFICATION {
        uuid id PK
        uuid user_id FK
        uuid sender_id FK
        string type
        string title
        text content
        boolean is_read
        timestamp read_at
        timestamp created_at
    }

    CAR_STYLE ||--o{ CAR_SERIES : "has"
    CAR_SERIES ||--o{ CAR_VERSION : "has"
    CAR_VERSION ||--o{ CAR : "based on"
    CAR_VERSION ||--o{ CAR_SPECIFICATION : "has"
    CAR_VERSION ||--o{ CAR_GALLERY : "has"
    CAR_VERSION ||--o{ CAR_PRICE : "priced by"
    EXTERIOR_COLOR ||--o{ CAR_PRICE : "used in"
    INTERIOR_COLOR ||--o{ CAR_PRICE : "used in"
    DEALERSHIP ||--o{ CAR : "stocks"
    DEALERSHIP ||--o{ SERVICE_TIME_SLOT : "defines"
    DEALERSHIP ||--o{ APPOINTMENT : "hosts"
    DEALERSHIP ||--o{ SERVICE : "handles"
    DEALERSHIP ||--o{ CUSTOMER_REVIEW : "receives"
    USER ||--o{ CAR : "owns"
    USER ||--o{ APPOINTMENT : "books"
    USER ||--o{ SERVICE : "requests"
    USER ||--o{ CHAT_SESSION : "owns"
    USER ||--o{ CHAT_MESSAGE : "sends"
    USER ||--o{ NOTIFICATION : "receives"
    GUEST_INFORMATION ||--o{ APPOINTMENT : "used in"
    GUEST_INFORMATION ||--o{ SERVICE : "used in"
    APPOINTMENT ||--o| SERVICE : "generates"
    APPOINTMENT ||--o| CUSTOMER_REVIEW : "triggers"
    SERVICE ||--o{ SERVICE_ITEM : "contains"
    SERVICE ||--o| CUSTOMER_REVIEW : "triggers"
    SERVICE_ITEM }o--|| ACCESSORY : "references"
    CHAT_SESSION ||--o{ CHAT_MESSAGE : "contains"
```

---

#### MongoDB — ai-service

Các collection phục vụ quản lý tài liệu AI, phiên chat RAG và job xử lý.

```mermaid
erDiagram
    ai_documents {
        string document_id PK
        ObjectId gridfs_file_id FK
        string filename
        string content_type
        int size_bytes
        string sha256
        datetime uploaded_at
        string uploaded_by_user_id
        string document_category
        string status
    }

    ai_pdfs {
        ObjectId _id PK
        string filename
        int length
        int chunk_size
        datetime upload_date
        string metadata
    }

    ai_document_jobs {
        string job_id PK
        string document_id FK
        string status
        string error_message
        datetime created_at
        datetime updated_at
    }

    ai_sessions {
        string session_id PK
        string user_id
        dict slots
        list history
        list intent_history
        string stage
        string last_intent
        int turn_count
        string status
        datetime created_at
        datetime updated_at
    }

    ai_chat_messages {
        string message_id PK
        string session_id FK
        string user_id
        string question
        string answer
        string intent
        string stage
        dict slots_snapshot
        list sources
        string model_used
        string rule_triggered
        datetime created_at
    }

    ai_documents ||--o{ ai_document_jobs : "tracked by"
    ai_documents ||--|| ai_pdfs : "binary stored in"
    ai_sessions ||--o{ ai_chat_messages : "contains"
```

> **Vector storage (Qdrant):** Các chunk PDF được embedding bằng `sentence-transformers` và lưu trong Qdrant. Mỗi vector point liên kết về `document_id` trong MongoDB để truy vết citation khi RAG retrieval.

---

### Chiến lược render Frontend

| Route | Chiến lược | Lý do |
|-------|------------|-------|
| `/` (Landing) | SSR + cache ngắn | SEO và first paint |
| `/vehicles` | SSR / dynamic fetch | Catalog xe với dữ liệu lọc động |
| `/vehicles/[id]` | SSR | Chi tiết xe, thông số, thư viện ảnh và phụ kiện |
| `/appointments/*` | SSR + client forms | Đặt lịch lái thử và lịch dịch vụ |
| `/support/live-chat` | Client-side | Live chat realtime qua WebSocket |
| `#ai-chat` widget | Client-side | Trợ lý AI nổi trong layout ứng dụng |
| `/dashboard/*` | SSR `force-dynamic` + client panels | Dashboard cá nhân theo vai trò |

---

### Cấu trúc dự án

```text
Tayota/
├── tayota-frontend/                  # Next.js 16.2.6 (App Router)
│   ├── src/
│   │   ├── app/
│   │   │   ├── appointments/
│   │   │   ├── auth/
│   │   │   ├── compare/
│   │   │   ├── dashboard/
│   │   │   ├── dealerships/
│   │   │   ├── news/
│   │   │   ├── notifications/
│   │   │   ├── reviews/
│   │   │   ├── support/live-chat/
│   │   │   ├── vehicles/
│   │   │   └── verify-account/
│   │   ├── components/
│   │   │   ├── layout/              # Header, Footer
│   │   │   ├── vehicles/            # Card xe, thông số, thư viện ảnh
│   │   │   ├── appointments/
│   │   │   ├── chat/
│   │   │   ├── dashboard/
│   │   │   ├── notifications/
│   │   │   └── reviews/
│   │   ├── lib/                     # API client, WebSocket, helpers
│   │   └── types/
│   ├── Dockerfile
│   ├── next.config.mjs
│   └── package.json
│
├── tayota-backend/
│   ├── docker-compose.yml           # Full infrastructure + services
│   ├── docker-compose.test.yml      # Test profile
│   └── docker-data/                 # Local data volumes
│
│   ├── api-gateway/                 # Spring Cloud Gateway WebFlux
│   │   ├── src/main/java/com/tayota/apigateway/
│   │   │   ├── config/              # CORS, routes, JWT filter
│   │   │   └── filter/              # Gateway filters, authentication
│   │   ├── src/main/resources/application.yml
│   │   ├── pom.xml
│   │   └── Dockerfile
│
│   ├── operation-service/           # Spring Boot 4 — core business logic
│   │   ├── src/main/java/com/tayota/operationservice/
│   │   │   ├── controller/          # REST + WebSocket controllers
│   │   │   ├── service/             # Business logic
│   │   │   ├── repository/          # JPA repositories
│   │   │   ├── entity/              # PostgreSQL entities
│   │   │   ├── dto/                 # Request/Response DTOs
│   │   │   ├── mapper/              # Entity <-> DTO mapping
│   │   │   └── config/              # Security, JWT, WebSocket, Cache
│   │   ├── src/main/resources/
│   │   │   ├── schema.sql           # Database schema
│   │   │   ├── data.sql             # Seed data (demo accounts)
│   │   │   └── application.properties
│   │   ├── pom.xml
│   │   └── Dockerfile
│
│   └── ai-service/                  # FastAPI — RAG chatbot
│       ├── app.py                   # API routes, middleware, health
│       ├── rag.py                   # Retrieval + generation pipeline
│       ├── vector_database.py       # Qdrant operations
│       ├── mongo_storage.py         # MongoDB/GridFS file storage
│       ├── conversation_state_manager.py
│       ├── documents/               # PDF source for RAG
│       ├── tests/
│       ├── requirements.txt
│       └── Dockerfile
│
├── README.md
└── LICENSE
```

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| **Frontend** | Next.js 16.2.6 (App Router), React 19.2.4, Tailwind CSS 4 |
| **HTTP Client** | Axios |
| **Realtime** | STOMP over WebSocket (frontend), Spring WebSocket (backend) |
| **API Gateway** | Java 21, Spring Boot 4.0.5, Spring Cloud Gateway 2025.1.1 WebFlux |
| **Auth** | JJWT 0.12.6, JWT stateless validation |
| **Operation Service** | Java 21, Spring Boot 4.0.5, Spring Security, Spring Data JPA |
| **Relational DB** | PostgreSQL 17, JPA/Hibernate |
| **Cache** | Caffeine (in-memory) |
| **AI Service** | Python 3.11, FastAPI 0.104.1, Uvicorn |
| **Vector DB** | Qdrant 1.7.4 |
| **Document Store** | MongoDB 8, GridFS |
| **Embeddings** | sentence-transformers 3.0.1 |
| **LLM** | Groq API (Llama 3.3 70B Versatile) |
| **Media Upload** | Cloudinary 2.3.2 |
| **Email** | Spring Mail + SMTP |
| **Containerization** | Docker Compose |
| **Testing** | JUnit 5, Mockito, H2 (in-memory), pytest |
| **Service Routing** | Spring Cloud Gateway routes HTTP/WebSocket |

---

## Yêu cầu

- **Docker Desktop** hoặc Docker Engine + Docker Compose.
- **Node.js** 20+ và **npm** 10+ nếu chạy frontend trên host.
- **Java** 21 và **Maven** nếu chạy Spring services độc lập.
- **Python** 3.11 nếu chạy AI service độc lập.

---

## Cài đặt

### 1. Clone repository

```bash
git clone <repo-url>
cd Tayota
```

### 2. Cấu hình biến môi trường

```bash
cd tayota-backend
cp .env.example .env   # Linux/macOS
# Windows PowerShell: Copy-Item .env.example .env
```

Chỉnh `.env` với giá trị của bạn:

```env
# ─── Ports ───
API_GATEWAY_PORT=9090
OPERATION_SERVICE_PORT=9091
AI_SERVICE_PORT=9094
FRONTEND_ORIGINS=http://localhost:3000

# ─── PostgreSQL ───
POSTGRES_USER=tayota
POSTGRES_PASSWORD=123456
POSTGRES_OPERATION_DB=tayota_operation_db

# ─── MongoDB ───
MONGO_USER=tayota
MONGO_PASSWORD=123456
MONGO_AI_DB=tayota_ai_db

# ─── JWT ───
JWT_SECRET=change-me-to-a-long-random-secret
GATEWAY_INTERNAL_SECRET=change-me-gateway-internal-secret

# ─── AI / LLM ───
LLM_PROVIDER=groq
GROQ_API_KEY=
GROQ_MODEL=llama-3.3-70b-versatile

# ─── Email (SMTP) ───
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=

# ─── Cloudinary ───
CLOUDINARY_CLOUD_NAME=
CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=
CLOUDINARY_FOLDER_PREFIX=tayota

# ─── Qdrant ───
QDRANT_API_KEY=
```

> **Ghi chú:** Docker Compose tự provision PostgreSQL, MongoDB và Qdrant trong internal network. Các biến AI/Groq/Cloudinary/SMTP có thể để trống khi phát triển local nếu chưa dùng các tích hợp đó.

### 3. Chạy ứng dụng

**Option A — Backend bằng Docker, frontend trên host** *(khuyến nghị cho development)*

```bash
cd tayota-backend
docker compose up --build -d

# Tùy chọn: nạp tài liệu AI vào vector index
docker compose exec ai-service python vector_database.py --rebuild --pdf-path /app/documents

# Terminal mới
cd tayota-frontend
npm ci
npm run dev
```

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| API Gateway | http://localhost:9090 |
| AI Service | http://localhost:9094 |
| Qdrant Dashboard | http://localhost:6333 |
| PostgreSQL | localhost:5432 |
| MongoDB | localhost:27017 |

**Option B — Docker hóa toàn bộ**

```bash
cd tayota-frontend
docker build -t tayota-frontend .

cd ../tayota-backend
docker compose up -d

# Theo dõi logs
docker compose logs -f api-gateway operation-service ai-service
```

> `Dockerfile` của frontend chạy độc lập với Compose. Nếu muốn tích hợp, thêm service `frontend` vào `docker-compose.yml` và cập nhật `FRONTEND_ORIGINS`.

**Option C — Chạy local hoàn toàn không dùng Docker**

```bash
# Terminal 1 — API Gateway
cd tayota-backend/api-gateway && ./mvnw spring-boot:run

# Terminal 2 — Operation Service
cd tayota-backend/operation-service && ./mvnw spring-boot:run

# Terminal 3 — AI Service
cd tayota-backend/ai-service
python -m venv .venv
source .venv/bin/activate      # Windows: .\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app:app --reload --port 9094

# Terminal 4 — Frontend
cd tayota-frontend && npm ci && npm run dev
```

Cần PostgreSQL 17, MongoDB 8 và Qdrant 1.7.4 đang chạy local.

---

## Tài khoản demo

`operation-service` tự seed tài khoản demo khi khởi động qua `data.sql`.

> **Mật khẩu mặc định:** `Tayota@123`

| Vai trò | Email | Mô tả |
|------|-------|-------|
| Admin | `admin.demo@tayota.com` | Toàn quyền hệ thống |
| Manager | `manager.demo@tayota.com` | Dashboard, nhân sự, catalog xe |
| Service Advisor | `advisor.demo@tayota.com` | Tiếp nhận lịch hẹn, tạo work order |
| Assistant | `assistant.demo@tayota.com` | Quản lý catalog, hỗ trợ advisor |
| Mechanic | `mechanic.demo@tayota.com` | Nhận và cập nhật work order |
| Customer | `customer.demo@tayota.com` | Đặt lịch, chat, đánh giá |

---

## Tài liệu API

### Gateway Routes

| Method | Endpoint | Service | Mô tả | Auth |
|--------|----------|---------|-------|------|
| POST | `/user/register` | operation-service | Đăng ký tài khoản | — |
| POST | `/user/login` | operation-service | Đăng nhập | — |
| POST | `/user/refresh-token` | operation-service | Refresh token | — |
| GET | `/user/me` | operation-service | Thông tin user hiện tại | User |
| POST | `/user/oauth/google` | operation-service | Đăng nhập Google OAuth | — |
| GET | `/user/profile/{userId}` | operation-service | Hồ sơ user | User/Staff |
| GET | `/car/catalog/car-styles-with-versions` | operation-service | Kiểu xe và phiên bản | — |
| GET | `/car/catalog/car-versions` | operation-service | Tìm kiếm/danh sách phiên bản xe | — |
| GET | `/car/catalog/car-versions/{id}` | operation-service | Chi tiết xe | — |
| GET | `/car/catalog/car-versions/{id}/specification` | operation-service | Thông số xe | — |
| POST | `/operation/appointments/test-drive` | operation-service | Tạo lịch lái thử | User |
| POST | `/operation/appointments/service` | operation-service | Tạo lịch dịch vụ | User |
| GET | `/operation/appointments/my` | operation-service | Lịch hẹn của khách hàng | User |
| GET | `/operation/appointments/advisor` | operation-service | Hàng đợi lịch hẹn của advisor | Staff |
| GET | `/operation/workorders/advisor` | operation-service | Work order của advisor | Staff |
| GET | `/operation/workorders/mechanic/my` | operation-service | Work order của mechanic | Mechanic |
| GET | `/ai/health` | ai-service | Health check AI | — |
| POST | `/ai/api/v1/chat` | ai-service | RAG chat | User/Guest |
| GET | `/ai/api/v1/documents` | ai-service | Danh sách tài liệu | Admin |
| POST | `/ai/api/v1/documents` | ai-service | Upload PDF và tạo job indexing | Admin |
| GET | `/ai/api/v1/documents/jobs/{jobId}` | ai-service | Trạng thái indexing tài liệu | Admin |

### WebSocket

| Path | Mô tả | Auth |
|------|------|------|
| `/user/chat/ws` | Live chat — customer ↔ advisor (STOMP) | User/Staff |

### AI Chat Endpoints

| Method | Endpoint | Mô tả | Auth |
|--------|----------|-------|------|
| POST | `/ai/api/v1/chat` | Gửi tin nhắn RAG chat | User/Guest |
| GET | `/ai/api/v1/users/{userId}/sessions` | Lịch sử session | User |
| GET | `/ai/api/v1/sessions/{sessionId}/messages` | Tin nhắn trong session | User |

### Admin Endpoints

| Method | Endpoint | Mô tả | Auth |
|--------|----------|-------|------|
| POST | `/user/create-account` | Tạo tài khoản nhân sự/khách hàng | Admin |
| GET | `/user/admin/users` | Danh sách user | Admin |
| GET | `/user/admin/users/stats` | Thống kê user | Admin |
| GET | `/user/admin/users/{userId}` | Chi tiết user | Admin |
| GET | `/ai/api/v1/documents` | Thư viện tài liệu AI | Admin |

---

## Kiểm thử

### Operation Service

```bash
cd tayota-backend/operation-service
./mvnw test

# Báo cáo coverage
./mvnw test jacoco:report
```

Hoặc qua Docker Compose test profile:

```bash
cd tayota-backend
docker compose -f docker-compose.test.yml --profile test up --build --abort-on-container-exit
```

### API Gateway

```bash
cd tayota-backend/api-gateway
./mvnw test
```

### AI Service

```bash
cd tayota-backend/ai-service
python -m venv .venv
source .venv/bin/activate      # Windows: .\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
pytest -v
```

### Frontend

```bash
cd tayota-frontend
npm run lint
npm run build
```

---

## Triển khai

| Component | Provider | Ghi chú |
|-----------|----------|---------|
| **Frontend** | Vercel / Render | Next.js static/SSR |
| **API Gateway** | Render | Spring Boot JAR, port `9090` |
| **Operation Service** | Render | Spring Boot JAR, port `9091` |
| **AI Service** | Render / Railway | FastAPI + Python, port `9094` |
| **Relational DB** | Render PostgreSQL / Neon | PostgreSQL 17 |
| **Vector DB** | Qdrant Cloud | v1.7.4+ |
| **Document Store** | MongoDB Atlas | M0 Free tier |
| **LLM** | Groq API | Llama 3.3 70B |
| **Media** | Cloudinary | Upload ảnh/video |
| **Email** | Gmail SMTP / Brevo | App Password |

### Checklist triển khai Render

1. **API Gateway** → New Web Service → JAR upload → Port `9090` → Health check `/actuator/health`
2. **Operation Service** → New Web Service → JAR upload → Port `9091` → Health check `/actuator/health`
3. **AI Service** → New Background Service → Start command: `uvicorn app:app --host 0.0.0.0 --port $PORT`

### Biến môi trường production bắt buộc

```env
# ─── Security ───
JWT_SECRET=<strong-random-32+-char-string>
GATEWAY_INTERNAL_SECRET=<gateway-internal-secret>
COOKIE_SECURE=true
COOKIE_SAME_SITE=Strict

# ─── Database ───
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:<port>/tayota_operation_db
SPRING_DATASOURCE_USERNAME=<user>
SPRING_DATASOURCE_PASSWORD=<password>

# ─── AI ───
GROQ_API_KEY=<key>
QDRANT_URL=https://<region>.qdrant.tech
QDRANT_API_KEY=<key>
MONGO_URI=mongodb+srv://<user>:<pass>@<cluster>.mongodb.net/tayota_ai_db

# ─── Frontend Origin ───
FRONTEND_ORIGINS=https://your-frontend.vercel.app
```

> Backend validate cấu hình khi startup và sẽ cảnh báo nếu `JWT_SECRET` yếu hoặc `COOKIE_SECURE=false` trong production.

---

## License

MIT License — xem [LICENSE](LICENSE) để biết chi tiết.

---

## Authors

Tayota Development Team
