# Giai đoạn 1: Cài đặt thư viện và chuẩn bị môi trường
# Sử dụng Node.js phiên bản 20 trên nền tảng Alpine (siêu nhẹ) để làm builder
FROM node:20-alpine AS builder

# Tạo thư mục /build bên trong Docker để làm việc
WORKDIR /build

# --- BƯỚC 1: Tải thư viện ---
# Chép file cấu hình thư viện (package.json và package-lock.json) từ máy vào Docker
# Việc chép file này trước giúp Docker cache lại layer này. Nếu code thay đổi mà thư viện không đổi, bước này sẽ được bỏ qua để build nhanh hơn
COPY package*.json ./
# build
#    ./ package.json
#    ./ package-lock.json

# Lệnh này tải tất cả các thư viện cần thiết về
# Dùng 'npm ci' thay vì 'npm install' vì nó cài đặt chính xác các phiên bản trong package-lock.json, giúp build nhanh và an toàn hơn
# --omit=dev: Chỉ cài đặt các thư viện dành cho môi trường production, bỏ qua các thư viện dev (như nodemon, jest...) để giảm bớt dung lượng
RUN npm ci --omit=dev

# --- BƯỚC 2: Copy Source Code ---
# Chép toàn bộ code nguồn vào Docker SAU KHI đã tải xong thư viện
COPY . .
# build
#    ./ package.json
#    ./ package-lock.json
#    ./ node_modules (sinh ra sau khi chạy npm ci)
#    ./ src
#    ./ server.js (giả sử đây là file khởi động của bạn)


# Giai đoạn 2: Run source code
# Sử dụng lại base image Node.js Alpine nhỏ gọn để chạy app thực tế
FROM node:20-alpine AS runner

# Thiết lập môi trường sản xuất (production) để Express.js tự động tối ưu hiệu suất và ẩn đi các lỗi chỉ dành cho dev
ENV NODE_ENV=production

# Làm việc trong thư mục /app của môi trường mới (Container runner là môi trường mới tinh)
WORKDIR /app
# app

# --- BƯỚC 3: Chuyển file từ builder sang runner ---
# Chép file package.json (cần thiết để Node.js đọc được các script và config)
COPY --from=builder /build/package*.json ./
# app
#    ./ package.json
#    ./ package-lock.json

# Chép thư mục thư viện (node_modules) đã được loại bỏ devDependencies từ bước 1
COPY --from=builder /build/node_modules ./node_modules
# app
#    ./ package.json
#    ./ node_modules

# Chép toàn bộ code nguồn đã được chuẩn bị
# Nếu code của bạn chỉ nằm trong thư mục /src và file server.js, bạn có thể sửa thành COPY --from=builder /build/src ./src để tối ưu hơn nữa
COPY --from=builder /build ./
# app
#    ./ package.json
#    ./ node_modules
#    ./ src
#    ./ server.js

# Chạy service bằng user 'node' có sẵn của image (thay vì root user) để tăng tính bảo mật
USER node

# Khởi chạy app trong môi trường Node.js
# Bạn thay đổi "server.js" thành tên file khởi động tương ứng trong dự án (ví dụ: index.js, app.js)
CMD ["node", "server.js"]