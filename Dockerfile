FROM node:20-alpine AS frontend-builder
WORKDIR /frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build


FROM gradle:8.5-jdk21 AS builder
WORKDIR /app
COPY . .

COPY --from=frontend-builder /frontend/dist /app/src/main/resources/static
RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]