# MAMS backend - Spring Boot on Java 21
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY backend/pom.xml .
RUN mvn -q -DskipTests dependency:go-offline || true
COPY backend/src src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/mams-backend-1.0.0.jar app.jar
EXPOSE 8080
CMD sh -c "java -XX:MaxRAMPercentage=75.0 -jar app.jar --server.port=${PORT:-8080}"
