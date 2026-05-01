# LLM Wiki Platform - Dockerfile
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app
COPY backend/pom.xml backend/pom.xml
COPY backend/llm-wiki-common/pom.xml backend/llm-wiki-common/
COPY backend/llm-wiki-adapter/pom.xml backend/llm-wiki-adapter/
COPY backend/llm-wiki-domain/pom.xml backend/llm-wiki-domain/
COPY backend/llm-wiki-service/pom.xml backend/llm-wiki-service/
COPY backend/llm-wiki-web/pom.xml backend/llm-wiki-web/

# Download dependencies first (cache layer)
RUN cd backend && mvn dependency:go-offline -B 2>/dev/null || true

# Copy source and build
COPY backend/ .
RUN cd backend && mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY --from=builder /app/backend/llm-wiki-web/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
