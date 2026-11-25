package com.thub.outpost;

import com.thub.outpost.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Smoke test: if the whole Spring application context starts (beans wired, Flyway migrated,
 * Hibernate schema validated against the real database), this passes.
 */
class ContextLoadsIT extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }
}
