# ---- Build stage: bien dich + dong goi jar bang Maven (JDK 21) ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Copy pom truoc de cache lop tai dependency (toi uu build).
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# Sinh khoa RS256 dev BAKE vao image (demo) -> app dung classpath:keys/*.pem, KHONG can secret rieng.
# Tradeoff: redeploy = khoa moi = token cu het hieu luc. Prod that nen inject khoa qua secret/file mount.
RUN apt-get update && apt-get install -y --no-install-recommends openssl \
    && mkdir -p src/main/resources/keys \
    && openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out src/main/resources/keys/jwt-private.pem \
    && openssl rsa -pubout -in src/main/resources/keys/jwt-private.pem -out src/main/resources/keys/jwt-public.pem
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
# Healthcheck: container "healthy" khi /actuator/health tra status UP. Dung ${PORT:-8080}
# de khop ca docker compose (8080) lan PaaS inject PORT (vd Render).
HEALTHCHECK --interval=15s --timeout=3s --start-period=45s --retries=3 \
    CMD curl -fsS http://localhost:${PORT:-8080}/actuator/health | grep -q '"status":"UP"' || exit 1
# MaxRAMPercentage: JVM ton trong gioi han RAM cua container (k8s limits).
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
