package com.thub.outpost.grant;

import static com.thub.outpost.support.GrantBuilder.aGrant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.thub.outpost.lookup.Sector;
import com.thub.outpost.lookup.SectorRepository;
import com.thub.outpost.lookup.Stage;
import com.thub.outpost.lookup.StageRepository;
import com.thub.outpost.support.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persistence of the Grant entity against the real schema.
 *
 * Each test is @Transactional: it runs inside a transaction that is rolled back at the end, so nothing
 * leaks into other tests. It is also needed for lazy loading: stages and sectors are loaded on first access,
 * which requires an open session (open-in-view is off, so there is no session outside a transaction).
 */
@Transactional
class GrantPersistenceIT extends AbstractIntegrationTest {

    @Autowired
    GrantRepository grants;

    @Autowired
    StageRepository stages;

    @Autowired
    SectorRepository sectors;

    @Autowired
    EntityManager entityManager;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void saveAndReload_keepsTwoStagesAndTwoSectors() {
        // The migration seeds the lookup rows, so we reuse them.
        List<Stage> twoStages = stages.findAll().subList(0, 2);
        List<Sector> twoSectors = sectors.findAll().subList(0, 2);
        Grant saved = grants.save(aGrant()
                .named("Women Entrepreneurship Scheme")
                .issuedBy("Ministry of MSME", IssuingBodyType.CENTRAL)
                .ofType(GrantType.SCHEME)
                .withDeadline(LocalDate.of(2026, 12, 31))
                .withStages(twoStages.toArray(new Stage[0]))
                .withSectors(twoSectors.toArray(new Sector[0]))
                .build());

        // Write to the database, then empty the first-level cache. Without clear(), findById would just
        // hand back the same Java object and prove nothing about what is stored.
        entityManager.flush();
        entityManager.clear();

        Grant reloaded = grants.findById(saved.getId()).orElseThrow();

        assertThat(reloaded).isNotSameAs(saved);
        assertThat(reloaded.getName()).isEqualTo("Women Entrepreneurship Scheme");
        assertThat(reloaded.getIssuingBodyType()).isEqualTo(IssuingBodyType.CENTRAL);
        assertThat(reloaded.getType()).isEqualTo(GrantType.SCHEME);
        assertThat(reloaded.getDeadline()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(reloaded.isPublished()).isFalse();
        assertThat(reloaded.isExpired()).isFalse();
        assertThat(reloaded.getStages()).containsExactlyInAnyOrderElementsOf(twoStages);
        assertThat(reloaded.getSectors()).containsExactlyInAnyOrderElementsOf(twoSectors);
    }

    @Test
    void rollingGrant_hasNullDeadline() {
        Grant saved = grants.save(aGrant().rolling().build());
        entityManager.flush();
        entityManager.clear();

        assertThat(grants.findById(saved.getId()).orElseThrow().getDeadline()).isNull();
    }

    @Test
    void enums_areStoredAsText_notNumbers() {
        grants.save(aGrant().issuedBy("Telangana Govt", IssuingBodyType.STATE).ofType(GrantType.SUBSIDY).build());
        entityManager.flush();

        Map<String, Object> row = jdbc.queryForMap("SELECT issuing_body_type, type FROM \"grant\"");

        assertThat(row).containsEntry("issuing_body_type", "STATE").containsEntry("type", "SUBSIDY");
    }

    @Test
    void auditTimestamps_areFilledOnSave() {
        Grant saved = grants.save(aGrant().build());
        entityManager.flush();

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void database_rejectsAnUnknownEnumValue() {
        // Bypasses JPA on purpose: proves the CHECK constraint in the migration protects the data.
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO "grant" (name, issuing_body, issuing_body_type, type, summary, source_url)
                VALUES ('x', 'y', 'GALACTIC', 'GRANT', 's', 'https://example.org')"""))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
