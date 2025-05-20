FROM maven:3-eclipse-temurin-17-alpine as maven_build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:17-alpine
WORKDIR /app
RUN mkdir -p /app/tmp/upload_answers_5494328847447246611
COPY answer-csv.csv /app/tmp/upload_answers_5494328847447246611/answer-csv.csv
COPY --from=maven_build /app/target/*.jar /app/Backend.jar
CMD ["java", "-jar", "/app/Backend.jar"]
EXPOSE 8080