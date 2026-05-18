# Vì common-core nằm cùng cấp với các Sevices nên đặt thư mục gốc (tayota-backend) bằng  context: . (ở docker-compose)

# Giai đoạn 1: Build và compile source code
# Sử dụng Maven kết hợp với JDK 21 trên nền tảng Alpine/Ubuntu để build
FROM maven:3.9.9-eclipse-temurin-21 AS builder

# Tạo thư mục /build bên trong Docker để làm việc
WORKDIR /build
# build

# --- Bước 1: Build common-core ---
# Thay vì chép toàn bộ thư mục common-core, ta chỉ chép file pom.xml trước
# Điều này giúp Docker cache lại layer tải thư viện. Nếu code trong common-core thay đổi nhưng pom.xml không đổi, layer này vẫn được giữ lại
COPY common-core/pom.xml ./common-core/
# build
#    ./ common-core
#       ./ pom.xml

# Tải trước các dependency cho common-core
RUN cd common-core && mvn dependency:go-offline -B

# Chép code nguồn của common-core vào SAU KHI đã tải xong thư viện
COPY common-core/src ./common-core/src
# build
#    ./ common-core
#       ./ pom.xml
#       ./ src

# Chuyển vào thư mục common-core và cài đặt module này vào kho Maven nội bộ (.m2) của Docker container để các service khác có thể gọi đến
# -DskipTests: Bỏ qua chạy Test khi build
RUN cd common-core && mvn clean install -DskipTests
# build
#    ./ common-core
#       ./ pom.xml
#       ./ src
#       ./ target (sinh ra sau khi cài đặt)


# --- BƯỚC 2: Build Service ---
WORKDIR /build/service
# build
#    ./ common-core
#    ./ service (Thư mục làm việc hiện tại)


# Chép file cấu hình thư viện (pom.xml) của service từ máy vào Docker
COPY car-service/pom.xml .
# build
#    ./ common-core
#    ./ service
#       ./ pom.xml

# Lệnh này Maven tải trước tất cả các thư viện cần thiết về
# Lúc này Maven sẽ nhận ra thư viện common-core đã được build ở bước trên
# go-offline: Việc này giúp các lần build sau nhanh hơn (bỏ qua giai đoạn tải) nếu không thay đổi pom.xml (cached)
# -B: container build không cần có terminal
RUN mvn dependency:go-offline -B
# build
#    ./ common-core
#    ./ service
#       ./ pom.xml

# Chép toàn bộ code nguồn (thư mục src) của service vào Docker
COPY car-service/src ./src
# build
#    ./ common-core
#    ./ service
#       ./ pom.xml
#       ./ src

# Maven xóa thư mục target cũ sau đó, biên dịch và đóng gói lại thư mục target mới từ thư viện pom.xml và src và đóng gói code mới thành file .jar
# /build/service/target/car-service-0.0.1-SNAPSHOT.jar (tên và phiên bản dựa trên file pom.xml)
# -DskipTests: Bỏ qua chạy Test khi build
RUN mvn clean package -DskipTests
# build
#    ./ common-core
#    ./ service
#       ./ pom.xml
#       ./ src
#       ./ target
#          ./ car-service-0.0.1-SNAPSHOT.jar

# Giai đoạn 2: Run source code
# Sử dụng JRE (Java Runtime Environment) thay vì JDK (biên dịch) để giảm dung lượng Image vì đã build xong file .jar từ bước 1
FROM eclipse-temurin:21-jre-alpine AS runner

# Làm việc trong thư mục /app của môi trường mới (Container runner là một môi trường hoàn toàn trống rỗng)
WORKDIR /app
# app

# Chép file .jar vừa được tạo ra từ giai đoạn builder sang giai đoạn runner và đổi tên thành app.jar
# Lưu ý: lấy đường dẫn trực tiếp từ cấu trúc thư mục của giai đoạn builder ở trên
COPY --from=builder /build/service/target/car-service-0.0.1-SNAPSHOT.jar ./app.jar
# app
#    ./ app.jar

# Khởi chạy app trong môi trường java, tìm tệp nén (-jar) từ app.jar
# Tìm META-INF/MANIFEST.MF (-jar), đọc dòng Main-Class để lấy ra tên class chính ví dụ AuthServiceApplication, rồi đi tìm file class tương ứng để chạy hàm Main
ENTRYPOINT ["java", "-jar", "app.jar"]