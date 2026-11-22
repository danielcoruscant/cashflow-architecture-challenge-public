FROM maven:3.9.12-eclipse-temurin-25 AS build

WORKDIR /workspace
COPY contracts/ ./contracts/
COPY services/consolidado-service/ ./services/consolidado-service/
WORKDIR /workspace/services/consolidado-service
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:25-jre-noble
WORKDIR /app
COPY --from=build /workspace/services/consolidado-service/target/consolidado-service-0.0.1-SNAPSHOT.jar /app/app.jar
COPY --from=build /workspace/services/consolidado-service/target/applicationinsights-agent.jar /app/applicationinsights-agent.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-javaagent:/app/applicationinsights-agent.jar", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
