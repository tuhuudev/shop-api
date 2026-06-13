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
# curl cho HEALTHCHECK + user khong-phai-root (giam rui ro neu container bi chiem quyen).
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd -r -u 1001 -s /usr/sbin/nologin appuser
COPY --from=build /app/target/*.jar app.jar
USER appuser
EXPOSE 8080
# Healthcheck: container "healthy" khi /actuator/health tra status UP.
HEALTHCHECK --interval=15s --timeout=3s --start-period=45s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1
# MaxRAMPercentage: JVM ton trong gioi han RAM cua container (k8s limits).
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
