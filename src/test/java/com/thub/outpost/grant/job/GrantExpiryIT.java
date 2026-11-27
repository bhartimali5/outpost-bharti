package com.thub.outpost.grant.job;

import static com.thub.outpost.support.GrantBuilder.aGrant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.thub.outpost.grant.entity.Grant;
import com.thub.outpost.grant.repository.GrantRepository;
import com.thub.outpost.support.AbstractIntegrationTest;
import com.thub.outpost.support.TestDataCleaner;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * OP-013 against a real Postgres: what the expiry job really changes, and the includeExpired listing flag.
 *
 * The job is built here with a FIXED clock (the scheduled one is switched off in the test profile), so the
 * dates in these tests never depend on the day the build runs.
 */
class GrantExpiryIT extends AbstractIntegrationTest {

    /** 20:00 UTC on 14 June = 01:30 on 15 June in India. So "today" is 15 June and "yesterday" 14 June. */
    private static final Instant NOW = Instant.parse("2026-06-14T20:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneId.of("Asia/Kolkata"));

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestDataCleaner cleaner;
    @Autowired
    GrantRepository grants;

    GrantExpiryJob job;

    @BeforeEach
    void setUp() {
        cleaner.clean("grant");
        job = new GrantExpiryJob(grants, FIXED_CLOCK);
    }

    // ---- the job ---------------------------------------------------------------------------------

    @Test
    void expiresYesterdaysGrants_andLeavesTodaysFutureAndRollingOnes() {
        UUID yesterday = save(aGrant().named("Yesterday").published().withDeadline(TODAY.minusDays(1)).build());
        UUID today = save(aGrant().named("Today").published().withDeadline(TODAY).build());
        UUID tomorrow = save(aGrant().named("Tomorrow").published().withDeadline(TODAY.plusDays(1)).build());
        UUID rolling = save(aGrant().named("Rolling").published().rolling().build());
        UUID oldDraft = save(aGrant().named("Old draft").withDeadline(TODAY.minusDays(30)).build());

        int expired = job.expireOverdueGrants();

        assertThat(expired).isEqualTo(2);
        assertThat(isExpired(yesterday)).isTrue();
        assertThat(isExpired(oldDraft)).isTrue(); // drafts expire too: expiry is about the deadline
        assertThat(isExpired(today)).isFalse(); // a deadline of today means still open today
        assertThat(isExpired(tomorrow)).isFalse();
        assertThat(isExpired(rolling)).isFalse(); // no deadline, never expires
    }

    @Test
    void usesTheIndianDate_notTheUtcDate() {
        // At NOW it is still 14 June in UTC. A UTC "today" would keep this grant open; India's 15 June expires it.
        UUID dueOn14June = save(aGrant().published().withDeadline(LocalDate.of(2026, 6, 14)).build());

        job.expireOverdueGrants();

        assertThat(isExpired(dueOn14June)).isTrue();
    }

    @Test
    void isIdempotent_aSecondRunExpiresNothing() {
        save(aGrant().published().withDeadline(TODAY.minusDays(1)).build());
        save(aGrant().published().withDeadline(TODAY.minusDays(2)).build());

        assertThat(job.expireOverdueGrants()).isEqualTo(2);
        assertThat(job.expireOverdueGrants()).isZero();
    }

    @Test
    void leavesAlreadyExpiredGrantsUntouched() {
        UUID alreadyExpired = save(aGrant().published().expired().withDeadline(TODAY.minusDays(5)).build());
        Instant updatedBefore = grants.findById(alreadyExpired).orElseThrow().getUpdatedAt();

        assertThat(job.expireOverdueGrants()).isZero();
        assertThat(grants.findById(alreadyExpired).orElseThrow().getUpdatedAt()).isEqualTo(updatedBefore);
    }

    @Test
    void setsUpdatedAt_onTheGrantsItExpires() {
        UUID id = save(aGrant().published().withDeadline(TODAY.minusDays(1)).build());

        job.expireOverdueGrants();

        assertThat(grants.findById(id).orElseThrow().getUpdatedAt()).isEqualTo(NOW);
    }

    // ---- includeExpired on the founder listing ---------------------------------------------------

    @Test
    void expiredGrantsAreHiddenByDefault_andShownWithIncludeExpired() throws Exception {
        save(aGrant().named("Active").published().build());
        save(aGrant().named("Expired").published().expired().build());

        listing().andExpect(jsonPath("$.content[*].name", contains("Active")));
        listing("includeExpired", "false").andExpect(jsonPath("$.content[*].name", contains("Active")));

        listing("includeExpired", "true", "sort", "name,asc")
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Active"))
                .andExpect(jsonPath("$.content[0].expired").value(false))
                .andExpect(jsonPath("$.content[1].name").value("Expired"))
                .andExpect(jsonPath("$.content[1].expired").value(true));
    }

    @Test
    void includeExpired_neverShowsDrafts() throws Exception {
        save(aGrant().named("Draft").build());
        save(aGrant().named("Expired draft").expired().build());
        save(aGrant().named("Expired").published().expired().build());

        listing("includeExpired", "true").andExpect(jsonPath("$.content[*].name", contains("Expired")));
    }

    @Test
    void includeExpired_combinesWithOtherFilters() throws Exception {
        save(aGrant().named("Women scheme, expired").published().expired().build());
        save(aGrant().named("Women scheme, active").published().build());
        save(aGrant().named("Other, expired").published().expired().build());

        listing("includeExpired", "true", "q", "women").andExpect(jsonPath("$.content[*].name",
                containsInAnyOrder("Women scheme, expired", "Women scheme, active")));
    }

    @Test
    void invalidIncludeExpiredValue_returns400() throws Exception {
        listing("includeExpired", "maybe").andExpect(status().isBadRequest());
    }

    @Test
    void afterTheJobRuns_theGrantDropsOffTheDefaultView_butIsNotLost() throws Exception {
        save(aGrant().named("Due yesterday").published().withDeadline(TODAY.minusDays(1)).build());
        listing().andExpect(jsonPath("$.content[*].name", contains("Due yesterday")));

        job.expireOverdueGrants();

        listing().andExpect(jsonPath("$.totalElements").value(0));
        listing("includeExpired", "true")
                .andExpect(jsonPath("$.content[0].name").value("Due yesterday"))
                .andExpect(jsonPath("$.content[0].expired").value(true));
    }

    // ---- helpers ---------------------------------------------------------------------------------

    private UUID save(Grant grant) {
        return grants.save(grant).getId();
    }

    private boolean isExpired(UUID id) {
        return grants.findById(id).orElseThrow().isExpired();
    }

    private ResultActions listing(String... keyValues) throws Exception {
        var request = get("/api/v1/grants").with(user("founder"));
        for (int i = 0; i < keyValues.length; i += 2) {
            request = request.param(keyValues[i], keyValues[i + 1]);
        }
        return mockMvc.perform(request);
    }
}
