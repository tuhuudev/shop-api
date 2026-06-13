package com.learn.shopapi;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Lop cha cho integration test (@SpringBootTest): chay tren PostgreSQL THAT qua Testcontainers,
 * KHONG dung H2. Nho the SQL native (vd to_char), kieu cot, Flyway... duoc kiem dung moi truong prod.
 *
 * SINGLETON CONTAINER: container la static + start MOT lan trong static block, KHONG dung @Container
 * (neu dung @Container, extension se stop() container sau test class DAU TIEN -> cac class sau dung
 * context cache lai mat ket noi "Connection refused"). Khong goi stop() -> Ryuk don khi JVM thoat.
 *
 * @ServiceConnection: Spring Boot tu tro datasource/redis toi container -> Flyway chay V1..V9.
 * @Testcontainers(disabledWithoutDocker=true): neu KHONG co Docker (vd Windows npipe), cac test SKIP.
 * Static block chi start khi Docker san co -> tranh ne ngoai le luc nap lop khi khong co Docker.
 */
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @ServiceConnection(name = "redis")
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7").withExposedPorts(6379);

    static {
        if (DockerClientFactory.instance().isDockerAvailable()) {
            POSTGRES.start();
            REDIS.start();
        }
    }
}
