# 1: Cài đặt Dependencies (deps)
# Sử dụng Node.js 20 Alpine để có sự cân bằng tốt nhất giữa hiệu năng và dung lượng
FROM node:20-alpine AS deps

# Alpine thiếu một số thư viện C++ cần thiết cho các package như 'sharp' hoặc 'swc'
# 'libc6-compat' giúp giải quyết vấn đề này mà không làm tăng dung lượng đáng kể
RUN apk add --no-cache libc6-compat

WORKDIR /app

# Chỉ copy các file quản lý thư viện để tận dụng cơ chế Cache của Docker.
# Nếu không thêm thư viện mới, Docker sẽ bỏ qua bước cài đặt (install) ở lần build sau.
COPY package.json yarn.lock* package-lock.json* pnpm-lock.yaml* ./

# Tự động nhận diện đang dùng npm, yarn hay pnpm để chạy lệnh cài đặt tương ứng
# --frozen-lockfile/--ci đảm bảo phiên bản thư viện cài đặt đúng như file lock
RUN \
  if [ -f yarn.lock ]; then yarn --frozen-lockfile; \
  elif [ -f package-lock.json ]; then npm ci; \
  elif [ -f pnpm-lock.yaml ]; then corepack enable pnpm && pnpm i --frozen-lockfile; \
  else echo "Không tìm thấy file lock" && exit 1; \
  fi


# 2: Build ứng dụng (builder)
FROM node:20-alpine AS builder
WORKDIR /app

# Copy thư mục node_modules đã cài đặt từ giai đoạn 1 sang
COPY --from=deps /app/node_modules ./node_modules

# Copy toàn bộ mã nguồn của bạn vào container
COPY . .

# Vô hiệu hóa tính năng thu thập dữ liệu ẩn danh của Next.js để build nhanh và riêng tư hơn
ENV NEXT_TELEMETRY_DISABLED 1

# Tiến hành build code React/Next.js thành code tối ưu cho Production
# Lệnh build này sẽ tạo ra thư mục .next/standalone (nhờ cấu hình ở Bước 0)
RUN \
  if [ -f yarn.lock ]; then yarn run build; \
  elif [ -f package-lock.json ]; then npm run build; \
  elif [ -f pnpm-lock.yaml ]; then corepack enable pnpm && pnpm run build; \
  else npm run build; \
  fi



#  3: Môi trường chạy thực tế
FROM node:20-alpine AS runner
WORKDIR /app

# Đặt môi trường là Production
ENV NODE_ENV production
ENV NEXT_TELEMETRY_DISABLED 1

# TẠO USER MỚI: Mặc định Docker chạy quyền 'root', rất nguy hiểm.
# Chúng ta tạo user 'nextjs' và group 'nodejs' để chạy app an toàn hơn.
RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

# Chỉ copy những file thực sự cần thiết để chạy ứng dụng.
# 1. Thư mục public: Chứa logo, ảnh tĩnh...
COPY --from=builder /app/public ./public

# 2. Thư mục standalone: Next.js đã gom toàn bộ code + node_modules cần thiết vào đây.
# Đây là "phép thuật" giúp Image của bạn cực nhẹ.
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./

# 3. Thư mục static: Chứa CSS, JS đã được compile cho phía trình duyệt.
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

# Chuyển quyền thực thi sang user 'nextjs'
USER nextjs

# Mở cổng 3000 cho container
EXPOSE 3000
ENV PORT 3000

# Đảm bảo container có thể kết nối được từ bên ngoài (thay vì chỉ localhost)
ENV HOSTNAME "0.0.0.0"

# Lệnh khởi chạy server.js (file này được Next.js sinh ra tự động trong standalone mode)
CMD ["node", "server.js"]