# Stage 1: Vue.js frontend build
FROM node:20-alpine AS frontend-builder
WORKDIR /frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build -- --outDir /frontend/dist

# Stage 2: Spring Boot build
FROM maven:3.9-eclipse-temurin-21-alpine AS backend-builder
WORKDIR /build
COPY db-management/ ./
COPY --from=frontend-builder /frontend/dist ./src/main/resources/static
RUN mvn clean package -DskipTests

# Stage 3: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend-builder /build/target/db-management-0.0.1-SNAPSHOT.jar ./app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
