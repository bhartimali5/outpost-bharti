package com.thub.outpost;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Smoke test: if the whole Spring application context starts (beans wired, Flyway migrated,
 * Hibernate schema validated), this passes.
 *
 * It starts its own throwaway Postgres in Docker, so it needs no running compose stack and
 * works the same on your machine and in CI. OP-008 moves this container into a shared base class.
 */
@SpringBootTest
@ActiveProfiles("test") // the "test" profile has no datasource URL; the container provides it
@Testcontainers
class OutpostApplicationTests {

    // @ServiceConnection tells Spring Boot to point the datasource at this container automatically.
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void contextLoads() {
    }
}
