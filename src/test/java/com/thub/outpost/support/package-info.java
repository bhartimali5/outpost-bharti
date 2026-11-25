/**
 * Shared test helpers.
 *
 * <ul>
 *   <li>{@link com.thub.outpost.support.AbstractIntegrationTest}: base class for integration tests
 *       (shared Postgres container, MockMvc).</li>
 *   <li>{@link com.thub.outpost.support.TestDataCleaner}: empties the tables a test touches.</li>
 *   <li>{@link com.thub.outpost.support.TestAuth}: mints JWTs (filled in with OP-017).</li>
 *   <li>Data builders: add one per entity as the entity is created, in this package, in the
 *       "Test Data Builder" style, with sensible defaults and fluent overrides, for example
 *       {@code aGrant().published().withDeadline(date).build()}. The first one arrives with
 *       the Grant entity (OP-009).</li>
 * </ul>
 */
package com.thub.outpost.support;
