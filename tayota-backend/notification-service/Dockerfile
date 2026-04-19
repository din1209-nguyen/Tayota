# Giai đoạn 1: Build và compile source code
# Sử dụng Maven kết hợp với JDK 21 trên nền tảng Alpine/Ubuntu để build
FROM maven:3.9.9-eclipse-temurin-21 AS builder

# Tạo thư mục /build bên trong Docker để làm việc
WORKDIR /build

# --- BƯỚC 1: Tải thư viện ---
# Chép file cấu hình thư viện (pom.xml) từ máy vào Docker
# Vì context lúc này là thư mục notification-service, ta chỉ cần gọi pom.xml
COPY pom.xml .
# build
#    ./ pom.xml

# Lệnh này Maven tải trước tất cả các thư viện cần thiết về
# go-offline: Việc này giúp các lần build sau nhanh hơn (bỏ qua giai đoạn tải) nếu không thay đổi pom.xml (cached)
# -B: container build không cần có terminal
RUN mvn dependency:go-offline -B

# --- BƯỚC 2: Build Service ---
# Chép toàn bộ code nguồn (thư mục src) vào Docker
COPY src ./src
# build
#    ./ pom.xml
#    ./ src

# Maven biên dịch và đóng gói code thành file .jar
# Kết quả sẽ sinh ra thư mục target: /build/target/notification-service-0.0.1-SNAPSHOT.jar
# -DskipTests: Bỏ qua chạy Test khi build
RUN mvn clean package -DskipTests
# build
#    ./ pom.xml
#    ./ src
#    ./ target
#       ./ notification-service-0.0.1-SNAPSHOT.jar


# Giai đoạn 2: Run source code
# Sử dụng JRE (Java Runtime Environment) thay vì JDK (biên dịch) để giảm dung lượng Image
FROM eclipse-temurin:21-jre-alpine AS runner

# Làm việc trong thư mục /app của môi trường mới (Container runner là một môi trường hoàn toàn trống rỗng)
WORKDIR /app
# app

# Chép file .jar vừa được tạo ra từ giai đoạn builder sang giai đoạn runner và đổi tên thành app.jar
# Lưu ý: Cập nhật lại đường dẫn từ /build/target/... (không còn thư mục /service/ ở giữa nữa)
COPY --from=builder /build/target/notification-service-0.0.1-SNAPSHOT.jar ./app.jar
# app
#    ./ app.jar

# Khởi chạy app trong môi trường java
ENTRYPOINT ["java", "-jar", "app.jar"]