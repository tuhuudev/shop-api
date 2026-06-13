package com.learn.shopapi;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Lop cha cho integration test (@SpringBootTest): chay tren PostgreSQL THAT qua Testcontainers,
 * KHONG dung H2. Nho the SQL native (vd to_char), kieu cot, Flyway... duoc kiem dung moi truong prod.
 *
 * @ServiceConnection: Spring Boot tu tro datasource toi container -> Flyway chay V1..V4 tao schema + seed.
 * @Testcontainers la @Inherited nen cac subclass dung chung container static nay (chi khoi 1 lan / JVM).
 *
 * disabledWithoutDocker=true: neu moi truong KHONG co Docker kha dung, cac test nay duoc SKIP (khong fail).
 * Huu ich cho Windows/Docker Desktop co luc khong ket noi duoc qua named pipe; tren CI (Linux) van chay day du.
 */
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection(name = "redis")
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7").withExposedPorts(6379);
}
