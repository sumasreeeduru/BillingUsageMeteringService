FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package dependency:copy-dependencies -DoutputDirectory=target/deps

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/classes ./classes
COPY --from=build /app/target/deps ./deps
EXPOSE 8080
CMD ["java", "-cp", "classes:deps/*", "usagebilling.Main"]
