# Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn
COPY src src
RUN chmod +x mvnw && ./mvnw -B -q package -DskipTests

# Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache wget \
    && addgroup -S glpi && adduser -S glpi -G glpi
USER glpi
COPY --from=build /app/target/glpi-integration-*.jar app.jar
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8081/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
