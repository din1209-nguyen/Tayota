#Để chạy được ứng dụng Spring Boot sử dụng Maven và Java 21 để biên dịch
FROM maven:3.9.9-eclipse-temurin-21 AS builder
#Tạo và di chuyển vào thư mục /app bên trong Docker để làm việc
WORKDIR /app
#Chép file cấu hình thư viện (pom.xml) từ máy vào Docker
COPY pom.xml .
#Lệnh này Maven tải trước tất cả các thư viện cần thiết về. Việc này giúp các lần build sau nhanh hơn (bỏ qua giai đoạn  tải) nếu không thay đổi pom.xml
RUN mvn dependency:go-offline -B
#Chép toàn bộ code nguồn (thư mục src) vào Docker
COPY src ./src
#Maven xóa thư mục target cũ sau đó build lại và đóng gói code mới thành file .jar
#/app/target/PatientManagementSystem-0.0.1-SNAPSHOT.jar (tên và phiên bản dựa trên file pom.xml)
RUN mvn clean package -DskipTests

#Sử dụng một môi trường chỉ có Java 21 (không có Maven) để chạy app cho nhẹ
FROM eclipse-temurin:21-jdk AS runner
#Làm việc trong thư mục /app của môi trường mới
WORKDIR /app
#Chép file .jar vừa được tạo ra từ giai đoạn builder sang giai đoạn runner và đổi tên thành app.jar cho gọn
COPY --from=builder ./app/target/api-gateway-0.0.1-SNAPSHOT.jar ./app.jar
#Ứng dụng chạy trên cổng 8081
EXPOSE 8079
#khởi động ứng dụng Java
ENTRYPOINT ["java", "-jar", "app.jar"]