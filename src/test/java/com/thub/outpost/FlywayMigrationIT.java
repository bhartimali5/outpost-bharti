package com.thub.outpost;

import static org.assertj.core.api.Assertions.assertThat;

import com.thub.outpost.support.AbstractIntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Proves tests run against the real schema: Flyway applied the migrations to the test container. */
class FlywayMigrationIT extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void baselineMigrationWasApplied() {
        List<Map<String, Object>> history = jdbc.queryForList(
                "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank");

        assertThat(history).isNotEmpty();
        assertThat(history.get(0)).containsEntry("version", "1").containsEntry("success", true);
        assertThat(history).allSatisfy(row -> assertThat(row).containsEntry("success", true));
    }

    @Test
    void baselineTableExists() {
        Integer rows = jdbc.queryForObject("SELECT count(*) FROM app_meta", Integer.class);

        assertThat(rows).isGreaterThanOrEqualTo(1);
    }
}
