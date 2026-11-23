package com.thub.outpost;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test: if the whole Spring application context starts (beans wired, Flyway migrated,
 * Hibernate schema validated), this passes.
 *
 * For now it uses the default "local" profile, so it needs `docker compose up -d` running.
 * OP-008 replaces this with a Testcontainers Postgres and the "test" profile.
 */
@SpringBootTest
class OutpostApplicationTests {

    @Test
    void contextLoads() {
    }
}
