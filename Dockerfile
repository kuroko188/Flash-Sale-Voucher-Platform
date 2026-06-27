# Build stage
FROM maven:3.9-eclipse-temurin-8 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn -B -DskipTests package

# Runtime stage
FROM eclipse-temurin:8-jre-jammy
WORKDIR /app
COPY --from=builder /app/target/hmdp-1.0-SNAPSHOT.jar app.jar

ENV SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/hmdp?useSSL=false&serverTimezone=UTC \
    SPRING_DATASOURCE_USERNAME=root \
    SPRING_DATASOURCE_PASSWORD=root \
    SPRING_REDIS_HOST=redis \
    SPRING_REDIS_PORT=6379 \
    SPRING_REDIS_PASSWORD=root

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
