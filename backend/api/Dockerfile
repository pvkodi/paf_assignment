FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /workspace

# Cache Maven dependencies separately from source code
# This layer only re-builds if pom.xml changes
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Now copy source and build
COPY . .
RUN mvn -DskipTests package -B

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /workspace/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
