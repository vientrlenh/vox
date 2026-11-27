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
# Match the time zone the tests run in (build.gradle sets user.timezone=Asia/Ho_Chi_Minh only
# for the test task). If it is not set here the container runs in UTC and every displayed timestamp
# is 7 hours off compared to the test environment -- a bug that is green on CI and red in real life.
ENV TZ=Asia/Ho_Chi_Minh
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]