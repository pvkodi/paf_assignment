FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /workspace
COPY . .
RUN mvn -DskipTests package -B

FROM eclipse-temurin:21-jre
USER root
# Install postgres client for readiness checks and bash for the entrypoint script
RUN apt-get update && apt-get install -y postgresql-client bash ca-certificates --no-install-recommends && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /workspace/target/*.jar app.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh
EXPOSE 8080
ENTRYPOINT ["/app/docker-entrypoint.sh"]
