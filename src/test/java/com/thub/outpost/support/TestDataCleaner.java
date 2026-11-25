package com.thub.outpost.support;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Empties the tables a test class touches, so tests are order-independent.
 *
 * <pre>
 * &#64;Autowired TestDataCleaner cleaner;
 *
 * &#64;BeforeEach
 * void clean() {
 *     cleaner.clean("grant", "founder");
 * }
 * </pre>
 *
 * Tables are named explicitly on purpose: seed data created by migrations (stage and sector rows, the
 * admin account) would be wiped by a "truncate everything" approach, and later tests would break.
 *
 * Because the container is shared by the whole test run, a test must not assume the tables are empty
 * at the start. Calling clean(...) in {@code @BeforeEach} makes that true.
 */
@Component
public class TestDataCleaner {

    private static final Pattern SAFE_NAME = Pattern.compile("[a-z_][a-z0-9_]*");

    private final JdbcTemplate jdbc;

    public TestDataCleaner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** TRUNCATE ... RESTART IDENTITY CASCADE: also empties tables that reference these ones. */
    public void clean(String... tables) {
        if (tables.length == 0) {
            return;
        }
        // Table names can't be bound as SQL parameters, so they are validated instead.
        // Quoted because some are reserved words in Postgres (the "grant" table!).
        String list = Arrays.stream(tables)
                .map(TestDataCleaner::checked)
                .map(name -> "\"" + name + "\"")
                .collect(Collectors.joining(", "));
        jdbc.execute("TRUNCATE TABLE " + list + " RESTART IDENTITY CASCADE");
    }

    private static String checked(String name) {
        if (!SAFE_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Not a valid table name: " + name);
        }
        if (name.equals("flyway_schema_history")) {
            throw new IllegalArgumentException("Never clean flyway_schema_history");
        }
        return name;
    }
}
