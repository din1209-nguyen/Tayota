# Giai đoạn 1: Build và compile source code
# Sử dụng hình ảnh Maven kết hợp với JDK 21 trên nền tảng Alpine/Ubuntu để build
FROM maven:3.9.9-eclipse-temurin-21 AS builder

#Tạo thư mục /app bên trong Docker để làm việc
WORKDIR /app
# app

#Chép file cấu hình thư viện (pom.xml) từ máy vào Docker
COPY pom.xml .
# app
#    ./ pom.xml

#Lệnh này Maven tải trước tất cả các thư viện cần thiết về.
#go-offline: Việc này giúp các lần build sau nhanh hơn (bỏ qua giai đoạn tải) nếu không thay đổi pom.xml (cached)
#-B: container build không cần có terminal
RUN mvn dependency:go-offline -B

#Chép toàn bộ code nguồn (thư mục src) vào Docker
COPY src ./src
# app
#    ./ pom.xml
#    ./ src

#Maven xóa thư mục target cũ sau đó, biên dịch và đóng gói lại thư mục target mới từ thư viện pom.xml và src và đóng gói code mới thành file .jar
#/app/target/auth-service-0.0.1-SNAPSHOT.jar ./app.jar (tên và phiên bản dựa trên file pom.xml)
# -DskipTests: Bỏ qua chạy Test vì giai đoạn test trong phần CI/CD rồi mới build docker
RUN mvn clean package -DskipTests
# app
#    ./ pom.xml
#    ./ src
#    ./ target/auth-service-0.0.1-SNAPSHOT.jar ./app.jar

# Giai đoạn 2: Run source code
#Sử dụng JRE (Java Runtime Environment) thay vì JDK (biên dịch) để giảm dung lượng Image vì đã build xong file .jar từ bước 1
FROM eclipse-temurin:21-jre-alpine AS runner

#Làm việc trong thư mục /app của môi trường mới
WORKDIR /app
# app

#Chép file .jar vừa được tạo ra từ giai đoạn builder sang giai đoạn runner và đổi tên thành app.jar
COPY --from=builder ./app/target/auth-service-0.0.1-SNAPSHOT.jar ./app.jar
# app
#    ./app.jar

#Ứng dụng chạy trên cổng 8083 trong docker
EXPOSE 8083

#Khởi chạy app trong môi trường java, tìm tệp nén (-jar) từ app.jar
#Tìm META-INF/MANIFEST.MF (-jar), đọc dòng Main-Class để lấy ra tên class chính ví dụ AuthServiceApplication, rồi đi tìm file class tương ứng để chạm hàm Main
ENTRYPOINT ["java", "-jar", "app.jar"]