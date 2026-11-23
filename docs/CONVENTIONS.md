# Conventions

Short rules we follow everywhere. Add to this file when a story introduces a new convention.

## Database migrations (Flyway)

The schema is owned by Flyway. Hibernate only checks it (`spring.jpa.hibernate.ddl-auto=validate`). Never use `update` or `create`.

- **Location:** `src/main/resources/db/migration/`
- **Name:** `V<n>__<snake_case_description>.sql`. Two underscores after the number. Examples: `V1__baseline.sql`, `V2__create_grant.sql`.
- **One migration per story** that changes the schema. Use the next free number.
- **Never edit a migration that has been merged.** Flyway stores a checksum of every applied file, and a changed file makes startup fail. To fix a mistake, add a new migration.
- **Runs on startup.** Applied versions are recorded in the `flyway_schema_history` table.
- **Failures are loud.** If a migration fails, the application does not start. Do not turn this off.
- **Local reset:** `docker compose down -v && docker compose up -d` gives a clean database, and Flyway re-applies everything.
- **Enums** are stored as `VARCHAR` and mapped with `@Enumerated(EnumType.STRING)`, never ordinal numbers.
- **Seed data** (lookup values and so on) also goes in a migration, so every environment gets the same data.
