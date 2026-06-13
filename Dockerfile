# ---- Build stage: bien dich + dong goi jar bang Maven (JDK 21) ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Copy pom truoc de cache lop tai dependency (toi uu build).
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- Run stage: chi can JRE 21, nhe ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
