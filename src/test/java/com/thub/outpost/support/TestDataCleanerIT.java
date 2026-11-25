package com.thub.outpost.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class TestDataCleanerIT extends AbstractIntegrationTest {

    @Autowired
    TestDataCleaner cleaner;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void createScratchTable() {
        // A scratch table, so this test can't wipe real data other tests rely on. "grant" mimics the
        // real table name that is a reserved word in Postgres.
        jdbc.execute("CREATE TABLE IF NOT EXISTS \"grant_cleaner_probe\" (id serial PRIMARY KEY, name text)");
        jdbc.update("INSERT INTO grant_cleaner_probe (name) VALUES ('a'), ('b')");
    }

    @Test
    void clean_emptiesTheNamedTableAndRestartsIds() {
        assertThat(count()).isEqualTo(2);

        cleaner.clean("grant_cleaner_probe");

        assertThat(count()).isZero();
        jdbc.update("INSERT INTO grant_cleaner_probe (name) VALUES ('c')");
        assertThat(jdbc.queryForObject("SELECT id FROM grant_cleaner_probe", Integer.class)).isEqualTo(1);
    }

    @Test
    void clean_rejectsUnsafeOrProtectedNames() {
        assertThatThrownBy(() -> cleaner.clean("grant; DROP TABLE app_meta"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cleaner.clean("flyway_schema_history"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Integer count() {
        return jdbc.queryForObject("SELECT count(*) FROM grant_cleaner_probe", Integer.class);
    }
}
