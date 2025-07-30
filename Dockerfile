# ─── Build/run image for a Spring Boot fat-jar ───────────────────────────
FROM eclipse-temurin:17-jdk-alpine

# copy the Maven-built jar (adjust if your JAR name ever changes)
ARG JAR_FILE=target/policy_service-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} /app.jar

ENTRYPOINT ["java","-jar","/app.jar"]
