-- Baseline migration (OP-003/OP-004).
-- Flyway runs each V<n>__description.sql file once, in version order, and records it in the
-- flyway_schema_history table. Never edit a migration after it has been merged; add a new one.
CREATE TABLE app_meta (
    key   VARCHAR(100) PRIMARY KEY,
    value VARCHAR(500) NOT NULL
);

INSERT INTO app_meta (key, value) VALUES ('schema_baseline', 'V1');
