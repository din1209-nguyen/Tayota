-- ============================================================
--  PostgreSQL Schema
--  Converted from DBML
-- ============================================================

-- Enable pgcrypto for gen_random_uuid() if needed (PostgreSQL < 13)
-- CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================
--  ENUMS
-- ============================================================

CREATE TYPE statustype AS ENUM ('UNVERIFIED', 'ACTIVE', 'BANNED');

CREATE TYPE roletype AS ENUM ('ADMIN', 'MANAGER', 'ASSISTANT', 'MECHANIC', 'USER');

CREATE TYPE providertype AS ENUM ('LOCAL', 'GOOGLE');

CREATE TYPE appointment_type AS ENUM (
  'TEST_DRIVE',
  'REPAIR',
  'MAINTENANCE',
  'PERIODIC_MAINTENANCE',
  'INSTALLATION'
);

CREATE TYPE car_status AS ENUM ('IN_TRANSIT', 'IN_STOCK', 'SOLD', 'MAINTENANCE');

CREATE TYPE contract_status AS ENUM ('DEPOSIT_PAID', 'FULL_PAID', 'DELIVERED', 'CANCELED');

CREATE TYPE customer_type_enum AS ENUM ('INDIVIDUAL', 'COMPANY');

CREATE TYPE payment_method_enum AS ENUM ('CASH', 'BANK_LOAN');

CREATE TYPE service_status_type AS ENUM (
  'CONFIRMED',
  'NEEDS_REASSIGNMENT',
  'RECEIVING',
  'IN_PROGRESS',
  'COMPLETED',
  'CANCELED',
  'EXPIRED'
);

CREATE TYPE item_type_enum AS ENUM ('LABOR', 'PART');

CREATE TYPE billing_type_enum AS ENUM ('NORMAL', 'WARRANTY', 'GIFT');

CREATE TYPE notification_type AS ENUM (
  'SYSTEM',
  'APPOINTMENT',
  'SERVICE',
  'MAINTENANCE_DUE',
  'PROMOTION',
  'DOCUMENT',
  'CHAT',
  'MANUAL_ALERT'
);

CREATE TYPE consultant_chat_status AS ENUM ('waiting', 'active', 'resolved', 'closed');

-- ============================================================
--  USER & PROFILE
-- ============================================================

CREATE TABLE "USER" (
  id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  email             VARCHAR(255)  NOT NULL UNIQUE,
  password_hash     VARCHAR(255),
  login_provider    provider_type NOT NULL DEFAULT 'LOCAL',
  provider_user_id  VARCHAR(120)  UNIQUE,
  role              role_type     NOT NULL DEFAULT 'USER',
  created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status            status_type   NOT NULL DEFAULT 'UNVERIFIED'
);
COMMENT ON TABLE "USER" IS 'Bảng người dùng gốc';
COMMENT ON COLUMN "USER".id               IS 'Mã định danh người dùng (UUID)';
COMMENT ON COLUMN "USER".email            IS 'Email';
COMMENT ON COLUMN "USER".password_hash    IS 'Mật khẩu mã hóa';
COMMENT ON COLUMN "USER".login_provider   IS 'Kiểu đăng nhập';
COMMENT ON COLUMN "USER".provider_user_id IS 'ID từ external provider';
COMMENT ON COLUMN "USER".role             IS 'Vai trò của người dùng';
COMMENT ON COLUMN "USER".status           IS 'Trạng thái hoạt động';

CREATE TABLE "USER_PROFILE" (
  id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  fullname    VARCHAR(40),
  phone       VARCHAR(10),
  gender      SMALLINT,       -- 0 = Nam, 1 = Nữ
  birth_date  DATE,
  address     VARCHAR(255),
  avatar_url  VARCHAR(255) DEFAULT '/default-avatar.png'
);
COMMENT ON COLUMN "USER_PROFILE".fullname   IS 'Tên hiển thị';
COMMENT ON COLUMN "USER_PROFILE".phone      IS 'Số điện thoại';
COMMENT ON COLUMN "USER_PROFILE".gender     IS 'Giới tính (0 = Nam, 1 = Nữ)';
COMMENT ON COLUMN "USER_PROFILE".birth_date IS 'Ngày sinh';
COMMENT ON COLUMN "USER_PROFILE".address    IS 'Địa chỉ liên hệ';

ALTER TABLE "USER_PROFILE"
  ADD CONSTRAINT fk_user_profile_user
  FOREIGN KEY (id) REFERENCES "USER"(id) ON DELETE CASCADE;

-- ============================================================
--  USER LOGIN HISTORY
-- ============================================================

CREATE TABLE "USER_LOGIN_HISTORY" (
  id          UUID  PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     UUID  NOT NULL,
  login_date  DATE  NOT NULL,
  UNIQUE (user_id, login_date)
);
COMMENT ON TABLE  "USER_LOGIN_HISTORY"            IS 'Lưu vết lịch sử đăng nhập theo ngày (Phục vụ tính năng Activity/Contribution Graph)';
COMMENT ON COLUMN "USER_LOGIN_HISTORY".login_date IS 'Ngày đăng nhập (Dùng để vẽ ô vuông xanh trên UI)';

ALTER TABLE "USER_LOGIN_HISTORY"
  ADD CONSTRAINT fk_login_history_user
  FOREIGN KEY (user_id) REFERENCES "USER"(id) ON DELETE CASCADE;

-- ============================================================
--  USER VIEW HISTORY
-- ============================================================

CREATE TABLE "USER_VIEW_HISTORY" (
  id              UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         UUID      NOT NULL,
  car_version_id  UUID      NOT NULL,
  viewed_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE  "USER_VIEW_HISTORY"           IS 'Lịch sử xem xe để phân tích hành vi và Gợi ý xe';
COMMENT ON COLUMN "USER_VIEW_HISTORY".viewed_at IS 'Thời điểm xem';

-- (Forward refs resolved after CAR_VERSION is created)

-- ============================================================
--  USER BEHAVIOR
-- ============================================================

CREATE TABLE "USER_BEHAVIOR" (
  id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     UUID,                         -- NULL = khách vãng lai / Guest
  action_type VARCHAR(50) NOT NULL,
  description VARCHAR(250) NOT NULL,
  created_at  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE  "USER_BEHAVIOR"             IS 'Theo dõi sự kiện và hành vi chi tiết để phục vụ Data Analytics và Recommendation System';
COMMENT ON COLUMN "USER_BEHAVIOR".user_id     IS 'Người dùng (Có thể NULL nếu là khách vãng lai/Guest)';
COMMENT ON COLUMN "USER_BEHAVIOR".action_type IS 'Loại hành vi (VD: search_car, filter_price, read_article, compare_car, config_color)';
COMMENT ON COLUMN "USER_BEHAVIOR".description IS 'Mô tả chi tiết';
COMMENT ON COLUMN "USER_BEHAVIOR".created_at  IS 'Thời điểm xảy ra hành vi';

CREATE INDEX idx_user_behavior_user_action ON "USER_BEHAVIOR" (user_id, action_type);

ALTER TABLE "USER_BEHAVIOR"
  ADD CONSTRAINT fk_behavior_user
  FOREIGN KEY (user_id) REFERENCES "USER"(id) ON DELETE CASCADE;

-- ============================================================
--  NOTIFICATION
-- ============================================================

CREATE TABLE "NOTIFICATION" (
  id         UUID              PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    UUID              NOT NULL,
  sender_id  UUID,
  type       notification_type NOT NULL DEFAULT 'SYSTEM',
  title      VARCHAR(250)      NOT NULL,
  content    TEXT              NOT NULL,
  is_read    SMALLINT          DEFAULT 0,
  read_at    TIMESTAMP         DEFAULT CURRENT_TIMESTAMP,
  created_at TIMESTAMP         DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON COLUMN "NOTIFICATION".user_id   IS 'Người dùng nhận thông báo';
COMMENT ON COLUMN "NOTIFICATION".sender_id IS 'Người dùng gửi thông báo';
COMMENT ON COLUMN "NOTIFICATION".type      IS 'Phân loại thông báo';
COMMENT ON COLUMN "NOTIFICATION".title     IS 'Tiêu đề thông báo';
COMMENT ON COLUMN "NOTIFICATION".content   IS 'Nội dung thông báo';
COMMENT ON COLUMN "NOTIFICATION".is_read   IS 'Trạng thái chưa đọc';

ALTER TABLE "NOTIFICATION"
  ADD CONSTRAINT fk_notification_user   FOREIGN KEY (user_id)   REFERENCES "USER"(id),
  ADD CONSTRAINT fk_notification_sender FOREIGN KEY (sender_id) REFERENCES "USER"(id);

-- ============================================================
--  CAR HIERARCHY: STYLE → SERIES → VERSION
-- ============================================================

CREATE TABLE "CAR_STYLE" (
  id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  name        VARCHAR(100) NOT NULL UNIQUE,
  description VARCHAR(250) NOT NULL
);
COMMENT ON TABLE  "CAR_STYLE"      IS 'Lưu trữ danh mục kiểu dáng xe';
COMMENT ON COLUMN "CAR_STYLE".name IS 'Tên kiểu dáng (VD: Sedan, SUV, Hatchback)';

CREATE TABLE "CAR_SERIES" (
  id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  car_style_id UUID         NOT NULL,
  name         VARCHAR(100) NOT NULL,
  description  VARCHAR(250) NOT NULL,
  created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE  "CAR_SERIES"             IS 'Lưu trữ các dòng xe thuộc các kiểu dáng khác nhau';
COMMENT ON COLUMN "CAR_SERIES".car_style_id IS 'Khóa ngoại tham chiếu đến STYLE';
COMMENT ON COLUMN "CAR_SERIES".name         IS 'Tên dòng xe (VD: 3 Series, C-Class)';

ALTER TABLE "CAR_SERIES"
  ADD CONSTRAINT fk_series_style
  FOREIGN KEY (car_style_id) REFERENCES "CAR_STYLE"(id) ON DELETE CASCADE;

CREATE TABLE "CAR_VERSION" (
  id            UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
  car_series_id UUID           NOT NULL,
  name          VARCHAR(50)    NOT NULL,
  sale_percent  DECIMAL(5,2)   DEFAULT 0.00 CHECK (sale_percent >= 0 AND sale_percent <= 100),
  model_year    INT            NOT NULL,
  video_url     VARCHAR(255),
  is_visible    BOOLEAN        NOT NULL DEFAULT TRUE,
  created_at    TIMESTAMP      DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE  "CAR_VERSION"              IS 'Lưu trữ thông tin định danh và cấu hình chung của từng phiên bản xe';
COMMENT ON COLUMN "CAR_VERSION".car_series_id IS 'Khóa ngoại tham chiếu đến SERIES';
COMMENT ON COLUMN "CAR_VERSION".name          IS 'Phiên bản xe (VD: 2.0 AT, Luxury)';
COMMENT ON COLUMN "CAR_VERSION".sale_percent  IS 'Phần trăm giảm giá (0-100)';
COMMENT ON COLUMN "CAR_VERSION".video_url     IS 'Đường dẫn video giới thiệu xe';
COMMENT ON COLUMN "CAR_VERSION".is_visible    IS 'Cho phép hiển thị phiên bản xe trên website';

ALTER TABLE "CAR_VERSION"
  ADD CONSTRAINT fk_version_series
  FOREIGN KEY (car_series_id) REFERENCES "CAR_SERIES"(id) ON DELETE CASCADE;

-- Now add forward-ref FK for USER_VIEW_HISTORY
ALTER TABLE "USER_VIEW_HISTORY"
  ADD CONSTRAINT fk_view_history_user        FOREIGN KEY (user_id)        REFERENCES "USER"(id)        ON DELETE CASCADE,
  ADD CONSTRAINT fk_view_history_car_version FOREIGN KEY (car_version_id) REFERENCES "CAR_VERSION"(id) ON DELETE CASCADE;

-- ============================================================
--  COLOR TABLES
-- ============================================================

CREATE TABLE "EXTERIOR_COLOR" (
  id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  color_name VARCHAR(50) NOT NULL UNIQUE
);
COMMENT ON TABLE  "EXTERIOR_COLOR"            IS 'Danh mục màu sơn ngoại thất';
COMMENT ON COLUMN "EXTERIOR_COLOR".color_name IS 'Tên màu ngoại thất (VD: Đỏ, Đen, Trắng)';

CREATE TABLE "INTERIOR_COLOR" (
  id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  color_name VARCHAR(50) NOT NULL UNIQUE
);
COMMENT ON TABLE  "INTERIOR_COLOR"            IS 'Danh mục màu chất liệu nội thất';
COMMENT ON COLUMN "INTERIOR_COLOR".color_name IS 'Tên màu nội thất (VD: Kem, Đen)';

-- ============================================================
--  CAR PRICE
-- ============================================================

CREATE TABLE "CAR_PRICE" (
  car_version_id    UUID           NOT NULL,
  exterior_color_id UUID           NOT NULL,
  interior_color_id UUID           NOT NULL,
  price             DECIMAL(15,2)  NOT NULL CHECK (price >= 0),
  ex_image_url      VARCHAR(255),
  in_image_url      VARCHAR(255),
  PRIMARY KEY (car_version_id, exterior_color_id, interior_color_id)
);
COMMENT ON TABLE  "CAR_PRICE"                   IS 'Quản lý giá và hình ảnh xe chi tiết theo từng tổ hợp màu sắc';
COMMENT ON COLUMN "CAR_PRICE".price             IS 'Giá bán niêm yết';
COMMENT ON COLUMN "CAR_PRICE".ex_image_url      IS 'Ảnh minh họa màu ngoại thất';
COMMENT ON COLUMN "CAR_PRICE".in_image_url      IS 'Ảnh minh họa màu nội thất';

ALTER TABLE "CAR_PRICE"
  ADD CONSTRAINT fk_price_version       FOREIGN KEY (car_version_id)    REFERENCES "CAR_VERSION"(id)    ON DELETE CASCADE,
  ADD CONSTRAINT fk_price_ext_color     FOREIGN KEY (exterior_color_id) REFERENCES "EXTERIOR_COLOR"(id) ON DELETE RESTRICT,
  ADD CONSTRAINT fk_price_int_color     FOREIGN KEY (interior_color_id) REFERENCES "INTERIOR_COLOR"(id) ON DELETE RESTRICT;

-- ============================================================
--  CAR GALLERY & ARTICLE
-- ============================================================

CREATE TABLE "CAR_GALLERY" (
  id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  car_version_id UUID         NOT NULL,
  image_url      VARCHAR(255) NOT NULL
);
COMMENT ON TABLE  "CAR_GALLERY"          IS 'Lưu trữ bộ sưu tập hình ảnh chi tiết của xe';
COMMENT ON COLUMN "CAR_GALLERY".image_url IS 'Đường dẫn tới hình ảnh';

ALTER TABLE "CAR_GALLERY"
  ADD CONSTRAINT fk_gallery_version
  FOREIGN KEY (car_version_id) REFERENCES "CAR_VERSION"(id) ON DELETE CASCADE;

CREATE TABLE "CAR_ARTICLE" (
  id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  car_version_id UUID,
  type           VARCHAR(50)  NOT NULL,
  title          VARCHAR(255) NOT NULL,
  content        TEXT         NOT NULL,
  image_url      VARCHAR(255),
  is_published   BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE  "CAR_ARTICLE"       IS 'Lưu trữ bài viết tin tức chung hoặc bài viết liên quan đến xe';
COMMENT ON COLUMN "CAR_ARTICLE".type  IS 'Loại thông tin (VD: Khuyến mãi, Tính năng nổi bật)';
COMMENT ON COLUMN "CAR_ARTICLE".title IS 'Tiêu đề thông tin';

ALTER TABLE "CAR_ARTICLE"
  ADD CONSTRAINT fk_article_version
  FOREIGN KEY (car_version_id) REFERENCES "CAR_VERSION"(id) ON DELETE CASCADE;

-- ============================================================
--  CAR SPECIFICATION
-- ============================================================

CREATE TABLE "CAR_SPECIFICATION" (
  car_version_id         UUID         PRIMARY KEY,
  origin                 VARCHAR(100) NOT NULL,
  fuel                   VARCHAR(50)  NOT NULL,
  number_of_seats        INT          NOT NULL CHECK (number_of_seats > 0),
  length                 INT          NOT NULL,
  width                  INT          NOT NULL,
  height                 INT          NOT NULL,
  capacity               INT,
  cylinder_capacity      VARCHAR(50),
  cylinder               INT,
  gearbox                VARCHAR(50),
  maximum_speed          INT,
  acceleration           VARCHAR(50),
  torque                 VARCHAR(100),
  gross_weight_allowance INT,
  trademarks             VARCHAR(100)
);
COMMENT ON TABLE  "CAR_SPECIFICATION"                        IS 'Lưu trữ chi tiết thông số kỹ thuật của từng chiếc xe';
COMMENT ON COLUMN "CAR_SPECIFICATION".origin                 IS 'Nguồn gốc xuất xứ (VD: Nhập khẩu, Lắp ráp)';
COMMENT ON COLUMN "CAR_SPECIFICATION".fuel                   IS 'Loại nhiên liệu (VD: Xăng, Dầu, Điện)';
COMMENT ON COLUMN "CAR_SPECIFICATION".number_of_seats        IS 'Số chỗ ngồi';
COMMENT ON COLUMN "CAR_SPECIFICATION".length                 IS 'Chiều dài tổng thể (mm)';
COMMENT ON COLUMN "CAR_SPECIFICATION".width                  IS 'Chiều rộng tổng thể (mm)';
COMMENT ON COLUMN "CAR_SPECIFICATION".height                 IS 'Chiều cao tổng thể (mm)';
COMMENT ON COLUMN "CAR_SPECIFICATION".capacity               IS 'Dung tích bình nhiên liệu hoặc pin';
COMMENT ON COLUMN "CAR_SPECIFICATION".cylinder_capacity      IS 'Dung tích xi lanh (VD: 1998 cc)';
COMMENT ON COLUMN "CAR_SPECIFICATION".cylinder               IS 'Số xi lanh động cơ';
COMMENT ON COLUMN "CAR_SPECIFICATION".gearbox                IS 'Loại hộp số (VD: 6 AT, CVT)';
COMMENT ON COLUMN "CAR_SPECIFICATION".maximum_speed          IS 'Tốc độ tối đa (km/h)';
COMMENT ON COLUMN "CAR_SPECIFICATION".acceleration           IS 'Khả năng tăng tốc 0-100km/h (giây)';
COMMENT ON COLUMN "CAR_SPECIFICATION".torque                 IS 'Mô men xoắn cực đại';
COMMENT ON COLUMN "CAR_SPECIFICATION".gross_weight_allowance IS 'Trọng lượng toàn tải cho phép (kg)';

ALTER TABLE "CAR_SPECIFICATION"
  ADD CONSTRAINT fk_spec_version
  FOREIGN KEY (car_version_id) REFERENCES "CAR_VERSION"(id) ON DELETE CASCADE;

-- ============================================================
--  ACCESSORY
-- ============================================================

CREATE TABLE "ACCESSORY" (
  id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  model            VARCHAR(100)  NOT NULL,
  brand            VARCHAR(100)  NOT NULL,
  price            DECIMAL(15,2) NOT NULL CHECK (price >= 0),
  description      TEXT,
  use_content      TEXT,
  reminder_content TEXT,
  type             VARCHAR(100) NOT NULL,
  is_visible       BOOLEAN      NOT NULL DEFAULT TRUE
);
COMMENT ON TABLE  "ACCESSORY"                 IS 'Danh mục các phụ kiện lắp thêm cho xe';
COMMENT ON COLUMN "ACCESSORY".model           IS 'Mã/Tên model phụ kiện';
COMMENT ON COLUMN "ACCESSORY".brand           IS 'Thương hiệu sản xuất';
COMMENT ON COLUMN "ACCESSORY".price           IS 'Giá bán phụ kiện';
COMMENT ON COLUMN "ACCESSORY".use_content     IS 'Hướng dẫn sử dụng';
COMMENT ON COLUMN "ACCESSORY".reminder_content IS 'Lưu ý khi bảo quản/sử dụng';

-- ============================================================
--  CAR ACCESSORY (Pivot)
-- ============================================================

CREATE TABLE "CAR_ACCESSORY" (
  car_version_id UUID NOT NULL,
  accessory_id   UUID NOT NULL,
  PRIMARY KEY (car_version_id, accessory_id)
);
COMMENT ON TABLE "CAR_ACCESSORY" IS 'Bảng trung gian (Pivot table) nối giữa xe và các phụ kiện tương thích';

ALTER TABLE "CAR_ACCESSORY"
  ADD CONSTRAINT fk_car_acc_version   FOREIGN KEY (car_version_id) REFERENCES "CAR_VERSION"(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_car_acc_accessory FOREIGN KEY (accessory_id)   REFERENCES "ACCESSORY"(id)   ON DELETE CASCADE;

-- ============================================================
--  DEALERSHIP
-- ============================================================

CREATE TABLE "DEALERSHIP" (
  id               UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
  name             VARCHAR(150)   NOT NULL,
  address          VARCHAR(255)   NOT NULL,
  car_quantity     INT            DEFAULT 0,
  latitude         DECIMAL(10,8)  NOT NULL,
  longitude        DECIMAL(11,8)  NOT NULL,
  place_id         VARCHAR(255)   UNIQUE,
  phone            VARCHAR(20),
  operating_hours  VARCHAR(100),
  is_active        BOOLEAN        NOT NULL DEFAULT TRUE,
  created_at       TIMESTAMP      DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE  "DEALERSHIP"                  IS 'Quản lý danh sách hệ thống cửa hàng hiển thị trên Bản đồ';
COMMENT ON COLUMN "DEALERSHIP".name             IS 'Tên cửa hàng/Dealership';
COMMENT ON COLUMN "DEALERSHIP".address          IS 'Địa chỉ hiển thị cho người dùng đọc';
COMMENT ON COLUMN "DEALERSHIP".car_quantity     IS 'Số lượng xe tồn kho';
COMMENT ON COLUMN "DEALERSHIP".latitude         IS 'Vĩ độ (Google Maps lat)';
COMMENT ON COLUMN "DEALERSHIP".longitude        IS 'Kinh độ (Google Maps lng)';
COMMENT ON COLUMN "DEALERSHIP".place_id         IS 'Google Place ID (Dùng để fetch đánh giá, hình ảnh từ GG API)';
COMMENT ON COLUMN "DEALERSHIP".phone            IS 'Số điện thoại hotline dealership';
COMMENT ON COLUMN "DEALERSHIP".operating_hours  IS 'Giờ mở cửa (VD: 08:00 - 20:00)';
COMMENT ON COLUMN "DEALERSHIP".is_active        IS 'Trạng thái hoạt động (1 = Đang mở, 0 = Đóng cửa)';

-- ============================================================
--  CAR (Physical unit)
-- ============================================================

CREATE TABLE "CAR" (
  vin_id          VARCHAR(17)  PRIMARY KEY,
  car_version_id  UUID         NOT NULL,
  dealership_id   UUID,
  engine_number   VARCHAR(50)  NOT NULL UNIQUE,
  status          car_status   NOT NULL DEFAULT 'IN_STOCK',
  mileage         INT          DEFAULT 0,
  production_year INT          NOT NULL,
  entry_date      DATE         NOT NULL,
  created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE  "CAR"                  IS 'Xe vật lý (mỗi row = 1 chiếc xe thực tế)';
COMMENT ON COLUMN "CAR".vin_id          IS 'Số khung xe (Vehicle Identification Number)';
COMMENT ON COLUMN "CAR".dealership_id   IS 'Xe đang nằm ở Dealership nào (NULL nếu xe đang In-transit từ nhà máy)';
COMMENT ON COLUMN "CAR".engine_number   IS 'Số máy (Động cơ)';
COMMENT ON COLUMN "CAR".status          IS 'Trạng thái vòng đời của xe vật lý';
COMMENT ON COLUMN "CAR".mileage         IS 'Số ODO hiện tại';
COMMENT ON COLUMN "CAR".entry_date      IS 'Ngày nhập kho/cảng';

CREATE INDEX idx_car_dealership_status ON "CAR" (dealership_id, status);

ALTER TABLE "CAR"
  ADD CONSTRAINT fk_car_version     FOREIGN KEY (car_version_id) REFERENCES "CAR_VERSION"(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_car_dealership  FOREIGN KEY (dealership_id)  REFERENCES "DEALERSHIP"(id)  ON DELETE SET NULL;

-- ============================================================
--  GUEST INFORMATION
-- ============================================================

CREATE TABLE "GUEST_INFORMATION" (
  id        UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  full_name VARCHAR(100) NOT NULL,
  email     VARCHAR(250) NOT NULL,
  phone     VARCHAR(10)  NOT NULL
);
COMMENT ON COLUMN "GUEST_INFORMATION".full_name IS 'Tên khách hàng';
COMMENT ON COLUMN "GUEST_INFORMATION".email     IS 'Email khách hàng';
COMMENT ON COLUMN "GUEST_INFORMATION".phone     IS 'SĐT khách hàng';

-- ============================================================
--  APPOINTMENT
-- ============================================================

CREATE TABLE "APPOINTMENT" (
  id                   UUID             PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id              UUID             NOT NULL,
  car_version_id       UUID             NOT NULL,
  dealership_id        UUID             NOT NULL,
  guest_information_id UUID             NOT NULL,
  type                 appointment_type NOT NULL,
  status               VARCHAR(20)      DEFAULT 'pending',
  scheduled_date       TIMESTAMP        NOT NULL,
  notes                TEXT,
  created_at           TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE  "APPOINTMENT"                IS 'Hành vi đặt lịch lái thử tại Dealership';
COMMENT ON COLUMN "APPOINTMENT".type           IS 'Dạng dịch vụ đặt lịch';
COMMENT ON COLUMN "APPOINTMENT".status         IS 'pending, confirmed, completed, canceled';
COMMENT ON COLUMN "APPOINTMENT".scheduled_date IS 'Ngày giờ hẹn lái thử';
COMMENT ON COLUMN "APPOINTMENT".notes          IS 'Ghi chú yêu cầu riêng của khách';

ALTER TABLE "APPOINTMENT"
  ADD CONSTRAINT fk_appt_user         FOREIGN KEY (user_id)              REFERENCES "USER"(id)              ON DELETE CASCADE,
  ADD CONSTRAINT fk_appt_car_version  FOREIGN KEY (car_version_id)       REFERENCES "CAR_VERSION"(id)       ON DELETE CASCADE,
  ADD CONSTRAINT fk_appt_dealership   FOREIGN KEY (dealership_id)        REFERENCES "DEALERSHIP"(id)        ON DELETE CASCADE,
  ADD CONSTRAINT fk_appt_guest        FOREIGN KEY (guest_information_id) REFERENCES "GUEST_INFORMATION"(id) ON DELETE CASCADE;

-- ============================================================
--  APPOINTMENT SCHEDULE CONFIGURATION
-- ============================================================

CREATE TABLE "SERVICE_TIME_SLOT" (
  id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  dealership_id    UUID        NOT NULL,
  appointment_type VARCHAR(40) NOT NULL,
  start_time       TIME        NOT NULL,
  end_time         TIME        NOT NULL,
  is_active        BOOLEAN     NOT NULL DEFAULT TRUE,
  created_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_service_time_slot_dealership_type_start
    UNIQUE (dealership_id, appointment_type, start_time),
  CONSTRAINT chk_service_time_slot_time
    CHECK (end_time > start_time)
);
COMMENT ON TABLE  "SERVICE_TIME_SLOT"                  IS 'Khung giờ đặt lịch theo từng đại lý và loại lịch hẹn';
COMMENT ON COLUMN "SERVICE_TIME_SLOT".dealership_id    IS 'Đại lý sở hữu khung giờ';
COMMENT ON COLUMN "SERVICE_TIME_SLOT".appointment_type IS 'Loại lịch: TEST_DRIVE hoặc SERVICE';
COMMENT ON COLUMN "SERVICE_TIME_SLOT".start_time       IS 'Giờ bắt đầu khung giờ';
COMMENT ON COLUMN "SERVICE_TIME_SLOT".end_time         IS 'Giờ kết thúc khung giờ';
COMMENT ON COLUMN "SERVICE_TIME_SLOT".is_active        IS 'Cho phép khách đặt khung giờ này hay không';

CREATE INDEX idx_service_time_slot_lookup
  ON "SERVICE_TIME_SLOT" (dealership_id, appointment_type, is_active, start_time);

ALTER TABLE "SERVICE_TIME_SLOT"
  ADD CONSTRAINT fk_service_time_slot_dealership
    FOREIGN KEY (dealership_id) REFERENCES "DEALERSHIP"(id) ON DELETE CASCADE;

CREATE TABLE "APPOINTMENT_HOLIDAY" (
  id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  dealership_id  UUID         NOT NULL,
  holiday_date   DATE         NOT NULL,
  reason         VARCHAR(255),
  is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_appointment_holiday_dealership_date
    UNIQUE (dealership_id, holiday_date)
);
COMMENT ON TABLE  "APPOINTMENT_HOLIDAY"               IS 'Ngày nghỉ không cho khách đặt lịch theo từng đại lý';
COMMENT ON COLUMN "APPOINTMENT_HOLIDAY".dealership_id IS 'Đại lý áp dụng ngày nghỉ';
COMMENT ON COLUMN "APPOINTMENT_HOLIDAY".holiday_date  IS 'Ngày không nhận lịch hẹn';
COMMENT ON COLUMN "APPOINTMENT_HOLIDAY".reason        IS 'Lý do nghỉ hoặc ghi chú nội bộ';
COMMENT ON COLUMN "APPOINTMENT_HOLIDAY".is_active     IS 'Ngày nghỉ còn hiệu lực hay không';

CREATE INDEX idx_appointment_holiday_lookup
  ON "APPOINTMENT_HOLIDAY" (dealership_id, holiday_date, is_active);

ALTER TABLE "APPOINTMENT_HOLIDAY"
  ADD CONSTRAINT fk_appointment_holiday_dealership
    FOREIGN KEY (dealership_id) REFERENCES "DEALERSHIP"(id) ON DELETE CASCADE;

-- ============================================================
--  MECHANIC
-- ============================================================

CREATE TABLE "MECHANIC" (
  id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  dealership_id  UUID          NOT NULL,
  specialty      VARCHAR(100),
  average_rating DECIMAL(3,2)  DEFAULT 0,
  is_active      SMALLINT      DEFAULT 1
);
COMMENT ON TABLE  "MECHANIC"                IS 'Thông tin đội ngũ kỹ thuật viên';
COMMENT ON COLUMN "MECHANIC".dealership_id  IS 'Trực thuộc Dealership/Xưởng nào';
COMMENT ON COLUMN "MECHANIC".specialty      IS 'Chuyên môn chính (Máy, Gầm, Điện, Sơn...)';
COMMENT ON COLUMN "MECHANIC".average_rating IS 'Điểm đánh giá trung bình';
COMMENT ON COLUMN "MECHANIC".is_active      IS 'Trạng thái làm việc';

ALTER TABLE "MECHANIC"
  ADD CONSTRAINT fk_mechanic_user       FOREIGN KEY (id)           REFERENCES "USER"(id),
  ADD CONSTRAINT fk_mechanic_dealership FOREIGN KEY (dealership_id) REFERENCES "DEALERSHIP"(id);

-- ============================================================
--  SERVICE
-- ============================================================

CREATE TABLE "SERVICE" (
  id                  UUID                PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id             UUID,
  guest_information_id UUID,
  vin_id              VARCHAR(17)         NOT NULL,
  mechanic_id         UUID,
  dealership_id       UUID                NOT NULL,
  appointment_id      UUID,
  mileage_at_service  INT                 NOT NULL,
  status              service_status_type,
  total_amount        DECIMAL(15,2)       DEFAULT 0,
  notes               TEXT,
  created_at          TIMESTAMP           DEFAULT CURRENT_TIMESTAMP,
  completed_at        TIMESTAMP
);
COMMENT ON TABLE  "SERVICE"                       IS 'Lưu trữ các phiên làm việc/sửa chữa xe';
COMMENT ON COLUMN "SERVICE".user_id               IS 'Khách hàng có tài khoản, có thể NULL nếu là khách vãng lai';
COMMENT ON COLUMN "SERVICE".guest_information_id  IS 'Thông tin khách vãng lai hoặc khách walk-in nếu không có tài khoản';
COMMENT ON COLUMN "SERVICE".vin_id                IS 'Xe cụ thể (Số khung)';
COMMENT ON COLUMN "SERVICE".mechanic_id           IS 'Thợ chính phụ trách';
COMMENT ON COLUMN "SERVICE".dealership_id         IS 'Địa điểm sửa chữa';
COMMENT ON COLUMN "SERVICE".appointment_id        IS 'Liên kết cuộc hẹn (NULL nếu khách vãng lai không đặt trước)';
COMMENT ON COLUMN "SERVICE".mileage_at_service    IS 'Số KM của xe khi vào xưởng';
COMMENT ON COLUMN "SERVICE".total_amount          IS 'Tổng tiền hóa đơn (Sum của các item)';
COMMENT ON COLUMN "SERVICE".notes                 IS 'Ghi chú tình trạng xe lúc nhận';

ALTER TABLE "SERVICE"
  ADD CONSTRAINT fk_service_user        FOREIGN KEY (user_id)        REFERENCES "USER"(id),
  ADD CONSTRAINT fk_service_guest_information FOREIGN KEY (guest_information_id) REFERENCES "GUEST_INFORMATION"(id),
  ADD CONSTRAINT fk_service_car         FOREIGN KEY (vin_id)         REFERENCES "CAR"(vin_id),
  ADD CONSTRAINT fk_service_mechanic    FOREIGN KEY (mechanic_id)    REFERENCES "MECHANIC"(id),
  ADD CONSTRAINT fk_service_dealership  FOREIGN KEY (dealership_id)  REFERENCES "DEALERSHIP"(id),
  ADD CONSTRAINT fk_service_appointment FOREIGN KEY (appointment_id) REFERENCES "APPOINTMENT"(id);

-- ============================================================
--  SERVICE ITEM
-- ============================================================

CREATE TABLE "SERVICE_ITEM" (
  id           UUID              PRIMARY KEY DEFAULT gen_random_uuid(),
  service_id   UUID              NOT NULL,
  item_type    item_type_enum    NOT NULL DEFAULT 'PART',
  accessory_id UUID,
  item_name    VARCHAR(255)      NOT NULL,
  quantity     INT               NOT NULL DEFAULT 1,
  unit_price   DECIMAL(15,2)     NOT NULL,
  billing_type billing_type_enum NOT NULL DEFAULT 'NORMAL',
  final_price  DECIMAL(15,2)     NOT NULL
);
COMMENT ON COLUMN "SERVICE_ITEM".accessory_id IS 'Mã phụ tùng/phụ kiện (NULL nếu item_type là LABOR)';
COMMENT ON COLUMN "SERVICE_ITEM".item_name    IS 'Tên hiển thị trên Bill (VD: Nhớt động cơ, Công thay nhớt)';
COMMENT ON COLUMN "SERVICE_ITEM".quantity     IS 'Số lượng hoặc số giờ công';
COMMENT ON COLUMN "SERVICE_ITEM".unit_price   IS 'Đơn giá niêm yết trên hệ thống';
COMMENT ON COLUMN "SERVICE_ITEM".billing_type IS 'Hình thức tính tiền';
COMMENT ON COLUMN "SERVICE_ITEM".final_price  IS 'Thành tiền thực tế thu của khách (0 nếu WARRANTY hoặc GIFT)';

ALTER TABLE "SERVICE_ITEM"
  ADD CONSTRAINT fk_service_item_service FOREIGN KEY (service_id) REFERENCES "SERVICE"(id) ON DELETE CASCADE;

-- ============================================================
--  CUSTOMER REVIEW
-- ============================================================

CREATE TABLE "CUSTOMER_REVIEW" (
  id                UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
  review_type       VARCHAR(30)  NOT NULL,
  status            VARCHAR(30)  NOT NULL,
  review_token      VARCHAR(80)  NOT NULL UNIQUE,
  token_expires_at  TIMESTAMP    NOT NULL,
  submitted_at      TIMESTAMP,
  appointment_id    UUID      UNIQUE,
  service_id        UUID      UNIQUE,
  user_id           UUID,
  guest_full_name   VARCHAR(100),
  guest_email       VARCHAR(120),
  guest_phone       VARCHAR(20),
  dealership_id     UUID      NOT NULL,
  service_rating    SMALLINT,
  service_comment   TEXT,
  mechanic_id       UUID,
  mechanic_rating   SMALLINT,
  mechanic_comment  TEXT,
  created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_customer_review_type
    CHECK (review_type IN ('TEST_DRIVE', 'SERVICE')),
  CONSTRAINT chk_customer_review_status
    CHECK (status IN ('PENDING', 'SUBMITTED', 'EXPIRED')),
  CONSTRAINT chk_customer_review_target
    CHECK (appointment_id IS NOT NULL OR service_id IS NOT NULL),
  CONSTRAINT chk_customer_review_service_rating
    CHECK (service_rating IS NULL OR service_rating BETWEEN 1 AND 5),
  CONSTRAINT chk_customer_review_mechanic_rating
    CHECK (mechanic_rating IS NULL OR mechanic_rating BETWEEN 1 AND 5),
  CONSTRAINT chk_customer_review_test_drive_no_mechanic
    CHECK (review_type <> 'TEST_DRIVE' OR (mechanic_id IS NULL AND mechanic_rating IS NULL AND mechanic_comment IS NULL))
);
COMMENT ON TABLE  "CUSTOMER_REVIEW"                IS 'Lưu đánh giá của khách hàng sau lịch lái thử hoặc dịch vụ sửa chữa';
COMMENT ON COLUMN "CUSTOMER_REVIEW".review_type    IS 'Loại đánh giá: TEST_DRIVE hoặc SERVICE';
COMMENT ON COLUMN "CUSTOMER_REVIEW".status         IS 'Trạng thái đánh giá: PENDING, SUBMITTED, EXPIRED';
COMMENT ON COLUMN "CUSTOMER_REVIEW".review_token   IS 'Token gửi trong email để khách đánh giá không cần đăng nhập';
COMMENT ON COLUMN "CUSTOMER_REVIEW".service_rating IS 'Số sao đánh giá trải nghiệm dịch vụ chung';
COMMENT ON COLUMN "CUSTOMER_REVIEW".mechanic_rating IS 'Số sao đánh giá thợ sửa, chỉ dùng cho dịch vụ sửa chữa';

ALTER TABLE "CUSTOMER_REVIEW"
  ADD CONSTRAINT fk_customer_review_appointment FOREIGN KEY (appointment_id) REFERENCES "APPOINTMENT"(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_customer_review_service     FOREIGN KEY (service_id)     REFERENCES "SERVICE"(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_customer_review_user        FOREIGN KEY (user_id)        REFERENCES "USER"(id),
  ADD CONSTRAINT fk_customer_review_dealership  FOREIGN KEY (dealership_id)  REFERENCES "DEALERSHIP"(id),
  ADD CONSTRAINT fk_customer_review_mechanic    FOREIGN KEY (mechanic_id)    REFERENCES "MECHANIC"(id);

-- ============================================================
--  SALES CONTRACT
-- ============================================================

CREATE TABLE "SALES_CONTRACT" (
  id                     UUID                 PRIMARY KEY DEFAULT gen_random_uuid(),
  contract_code          VARCHAR(50)          NOT NULL UNIQUE,
  user_id                UUID                 NOT NULL,
  customer_type          customer_type_enum   NOT NULL DEFAULT 'INDIVIDUAL',
  buyer_name             VARCHAR(100)         NOT NULL,
  identity_number        VARCHAR(20),
  tax_code               VARCHAR(50),
  vin_id                 VARCHAR(17)          NOT NULL,
  dealership_id          UUID                 NOT NULL,
  assistant_id           UUID                 NOT NULL,
  base_price             DECIMAL(15,2)        NOT NULL,
  accessory_amount       DECIMAL(15,2)        DEFAULT 0,
  service_fee            DECIMAL(15,2)        DEFAULT 0,
  vat_rate               DECIMAL(5,2)         DEFAULT 10,
  total_amount           DECIMAL(15,2)        NOT NULL,
  payment_method         payment_method_enum  NOT NULL DEFAULT 'CASH',
  deposit_amount         DECIMAL(15,2)        DEFAULT 0,
  status                 contract_status      NOT NULL,
  contract_date          DATE                 NOT NULL,
  expected_delivery_date DATE,
  actual_delivery_date   DATE,
  warranty_end_date      DATE,
  created_at             TIMESTAMP            DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON COLUMN "SALES_CONTRACT".contract_code          IS 'Mã hợp đồng in trên giấy';
COMMENT ON COLUMN "SALES_CONTRACT".user_id                IS 'Tài khoản hệ thống của khách';
COMMENT ON COLUMN "SALES_CONTRACT".buyer_name             IS 'Tên người mua hoặc Tên công ty';
COMMENT ON COLUMN "SALES_CONTRACT".identity_number        IS 'CCCD (cá nhân)';
COMMENT ON COLUMN "SALES_CONTRACT".tax_code               IS 'Mã số thuế (Khách doanh nghiệp)';
COMMENT ON COLUMN "SALES_CONTRACT".vin_id                 IS 'Chiếc xe vật lý được bán';
COMMENT ON COLUMN "SALES_CONTRACT".dealership_id          IS 'Dealership thực hiện giao dịch';
COMMENT ON COLUMN "SALES_CONTRACT".assistant_id           IS 'Tài khoản Sale/Trợ lý phụ trách chốt hợp đồng này';
COMMENT ON COLUMN "SALES_CONTRACT".base_price             IS 'Giá niêm yết';
COMMENT ON COLUMN "SALES_CONTRACT".accessory_amount       IS 'Tiền phụ kiện bán thêm';
COMMENT ON COLUMN "SALES_CONTRACT".service_fee            IS 'Phí dịch vụ như đăng ký, biển số...';
COMMENT ON COLUMN "SALES_CONTRACT".vat_rate               IS 'Thuế VAT (%)';
COMMENT ON COLUMN "SALES_CONTRACT".total_amount           IS 'Tổng tiền phải thu';
COMMENT ON COLUMN "SALES_CONTRACT".deposit_amount         IS 'Tiền cọc đã thu';
COMMENT ON COLUMN "SALES_CONTRACT".contract_date          IS 'Ngày ký';
COMMENT ON COLUMN "SALES_CONTRACT".expected_delivery_date IS 'Ngày hẹn giao xe';
COMMENT ON COLUMN "SALES_CONTRACT".actual_delivery_date   IS 'Ngày thực tế giao xe';
COMMENT ON COLUMN "SALES_CONTRACT".warranty_end_date      IS 'Ngày hết hạn bảo hành';

ALTER TABLE "SALES_CONTRACT"
  ADD CONSTRAINT fk_contract_user        FOREIGN KEY (user_id)      REFERENCES "USER"(id)        ON DELETE RESTRICT,
  ADD CONSTRAINT fk_contract_assistant   FOREIGN KEY (assistant_id) REFERENCES "USER"(id)        ON DELETE RESTRICT,
  ADD CONSTRAINT fk_contract_car         FOREIGN KEY (vin_id)       REFERENCES "CAR"(vin_id)     ON DELETE RESTRICT,
  ADD CONSTRAINT fk_contract_dealership  FOREIGN KEY (dealership_id) REFERENCES "DEALERSHIP"(id) ON DELETE RESTRICT;

-- ============================================================
--  SALES CONTRACT ACCESSORY
-- ============================================================

CREATE TABLE "SALES_CONTRACT_ACCESSORY" (
  contract_id  UUID          NOT NULL,
  accessory_id UUID          NOT NULL,
  quantity     INT           NOT NULL DEFAULT 1,
  price        DECIMAL(15,2) NOT NULL,
  is_gift      SMALLINT      DEFAULT 0,
  PRIMARY KEY (contract_id, accessory_id)
);
COMMENT ON TABLE  "SALES_CONTRACT_ACCESSORY"          IS 'Lưu danh sách phụ kiện khách hàng mua kèm (hoặc được tặng) khi ký hợp đồng mua xe';
COMMENT ON COLUMN "SALES_CONTRACT_ACCESSORY".contract_id  IS 'Thuộc hợp đồng bán xe nào';
COMMENT ON COLUMN "SALES_CONTRACT_ACCESSORY".accessory_id IS 'Mã phụ kiện được mua';
COMMENT ON COLUMN "SALES_CONTRACT_ACCESSORY".quantity     IS 'Số lượng mua';
COMMENT ON COLUMN "SALES_CONTRACT_ACCESSORY".price        IS 'Giá bán thực tế tại thời điểm chốt (Có thể khác giá niêm yết)';
COMMENT ON COLUMN "SALES_CONTRACT_ACCESSORY".is_gift      IS 'Đánh dấu nếu đây là phụ kiện tặng kèm (unit_price = 0)';

ALTER TABLE "SALES_CONTRACT_ACCESSORY"
  ADD CONSTRAINT fk_sca_contract  FOREIGN KEY (contract_id)  REFERENCES "SALES_CONTRACT"(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_sca_accessory FOREIGN KEY (accessory_id) REFERENCES "ACCESSORY"(id)       ON DELETE RESTRICT;

-- ============================================================
--  SUPPORT
-- ============================================================

CREATE TABLE "SUPPORT" (
  id          UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
  customer_id UUID      NOT NULL,
  content     TEXT,
  created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE  "SUPPORT"             IS 'Đơn hỗ trợ của người dùng gửi đến';
COMMENT ON COLUMN "SUPPORT".customer_id IS 'Khách hàng cần hỗ trợ';
COMMENT ON COLUMN "SUPPORT".content     IS 'Nội dung hỗ trợ mà khách hàng gửi';

ALTER TABLE "SUPPORT"
  ADD CONSTRAINT fk_support_user FOREIGN KEY (customer_id) REFERENCES "USER"(id);

-- ============================================================
--  AI CHAT
-- ============================================================

CREATE TABLE "AI_CHAT_SESSION" (
  id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      UUID,
  title        VARCHAR(255),
  context_data JSONB,
  created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE  "AI_CHAT_SESSION"              IS 'Quản lý phiên trò chuyện với Trợ lý AI';
COMMENT ON COLUMN "AI_CHAT_SESSION".user_id      IS 'ID khách hàng (NULL nếu là khách vãng lai chưa đăng nhập)';
COMMENT ON COLUMN "AI_CHAT_SESSION".title        IS 'Tiêu đề phiên chat (AI tự động tóm tắt)';
COMMENT ON COLUMN "AI_CHAT_SESSION".context_data IS 'Bối cảnh: Khách đang đứng ở URL nào, xem xe nào để AI hiểu ngữ cảnh';

ALTER TABLE "AI_CHAT_SESSION"
  ADD CONSTRAINT fk_ai_session_user FOREIGN KEY (user_id) REFERENCES "USER"(id) ON DELETE SET NULL;

CREATE TABLE "AI_CHAT_MESSAGE" (
  id          UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
  session_id  UUID      NOT NULL,
  user_message TEXT     NOT NULL,
  ai_message   TEXT     NOT NULL,
  created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE  "AI_CHAT_MESSAGE"              IS 'Chi tiết tin nhắn trong phiên chat với AI';
COMMENT ON COLUMN "AI_CHAT_MESSAGE".user_message IS 'Nội dung chat của user';
COMMENT ON COLUMN "AI_CHAT_MESSAGE".ai_message   IS 'Nội dung chat của AI';

ALTER TABLE "AI_CHAT_MESSAGE"
  ADD CONSTRAINT fk_ai_message_session FOREIGN KEY (session_id) REFERENCES "AI_CHAT_SESSION"(id) ON DELETE CASCADE;

-- ============================================================
--  CONSULTANT CHAT
-- ============================================================

CREATE TABLE "CONSULTANT_CHAT_SESSION" (
  id            UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id       UUID      NOT NULL,
  consultant_id UUID,
  topic         VARCHAR(100),
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE  "CONSULTANT_CHAT_SESSION"               IS 'Quản lý phiên hỗ trợ 1-1 giữa Khách hàng và Chuyên viên';
COMMENT ON COLUMN "CONSULTANT_CHAT_SESSION".user_id       IS 'Khách hàng (Bắt buộc phải đăng nhập hoặc để lại SĐT)';
COMMENT ON COLUMN "CONSULTANT_CHAT_SESSION".consultant_id IS 'ID của Chuyên viên phụ trách ca này';
COMMENT ON COLUMN "CONSULTANT_CHAT_SESSION".topic         IS 'Chủ đề: Báo giá, Kỹ thuật, Bảo hành...';

ALTER TABLE "CONSULTANT_CHAT_SESSION"
  ADD CONSTRAINT fk_consult_session_user       FOREIGN KEY (user_id)       REFERENCES "USER"(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_consult_session_consultant FOREIGN KEY (consultant_id) REFERENCES "USER"(id) ON DELETE SET NULL;

CREATE TABLE "CONSULTANT_CHAT_MESSAGE" (
  id         UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
  session_id UUID      NOT NULL,
  sender_id  UUID      NOT NULL,
  content    TEXT      NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE  "CONSULTANT_CHAT_MESSAGE"           IS 'Lưu trữ tin nhắn người thật';
COMMENT ON COLUMN "CONSULTANT_CHAT_MESSAGE".sender_id IS 'ID người gửi (khách hoặc tư vấn viên)';
COMMENT ON COLUMN "CONSULTANT_CHAT_MESSAGE".content   IS 'Nội dung tin nhắn';

ALTER TABLE "CONSULTANT_CHAT_MESSAGE"
  ADD CONSTRAINT fk_consult_msg_session FOREIGN KEY (session_id) REFERENCES "CONSULTANT_CHAT_SESSION"(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_consult_msg_sender  FOREIGN KEY (sender_id)  REFERENCES "USER"(id);
