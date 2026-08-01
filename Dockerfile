# syntax=docker/dockerfile:1.7

# Build stage
FROM gradle:9.5-jdk25 AS build
WORKDIR /workspace

COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./

RUN chmod +x gradlew

# Warm up Gradle dependency cache
RUN --mount=type=cache,target=/home/gradle/.gradle \
    ./gradlew dependencies --no-daemon || true

COPY src ./src

RUN --mount=type=cache,target=/home/gradle/.gradle \
    ./gradlew bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:25-jre-jammy
WORKDIR /app

COPY --from=build /workspace/build/libs/*.jar app.jar

EXPOSE 8080
# Khớp với múi giờ mà test chạy (build.gradle đặt user.timezone=Asia/Ho_Chi_Minh chỉ
# cho task test). Không đặt ở đây thì container chạy UTC và mọi mốc thời gian hiển thị
# lệch 7 tiếng so với môi trường test — loại lỗi xanh trên CI, đỏ ngoài đời.
ENV TZ=Asia/Ho_Chi_Minh
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]