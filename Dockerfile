FROM eclipse-temurin:17-jdk-alpine

WORKDIR /tmp

COPY target/policy_service-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]