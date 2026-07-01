FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build/service

COPY operation-service/pom.xml .
RUN mvn dependency:go-offline -B

COPY operation-service/src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app
COPY --from=builder /build/service/target/operation-service-0.0.1-SNAPSHOT.jar ./app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
