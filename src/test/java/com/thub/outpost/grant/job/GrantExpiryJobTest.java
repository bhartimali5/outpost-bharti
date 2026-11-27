package com.thub.outpost.grant.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thub.outpost.grant.repository.GrantRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Unit test: no Spring, no database. The repository is a Mockito mock, so this test checks the job's OWN logic:
 * which "today" it computes from the Clock, what it asks the repository to do, and what it logs.
 * Which rows actually expire (yesterday yes; today and rolling no) is decided by the UPDATE statement, so that is
 * proven against a real Postgres in GrantExpiryIT.
 */
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class GrantExpiryJobTest {

    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");

    @Mock
    GrantRepository grants;

    @Test
    void expiresGrantsDueBeforeTodayInIndia_usingTheInjectedClock() {
        // 20:00 UTC on 14 June is already 01:30 on 15 June in India, so "today" must be 15 June.
        Instant now = Instant.parse("2026-06-14T20:00:00Z");
        Clock clock = Clock.fixed(now, INDIA);
        when(grants.expireGrantsWithDeadlineBefore(LocalDate.of(2026, 6, 15), now)).thenReturn(3);

        int expired = new GrantExpiryJob(grants, clock).expireOverdueGrants();

        assertThat(expired).isEqualTo(3);
        verify(grants).expireGrantsWithDeadlineBefore(LocalDate.of(2026, 6, 15), now);
    }

    @Test
    void logsHowManyGrantsItExpired(CapturedOutput output) {
        Instant now = Instant.parse("2026-06-15T06:00:00Z");
        when(grants.expireGrantsWithDeadlineBefore(LocalDate.of(2026, 6, 15), now)).thenReturn(2);

        new GrantExpiryJob(grants, Clock.fixed(now, INDIA)).expireOverdueGrants();

        assertThat(output.getOut()).contains("expired 2 grant(s) with a deadline before 2026-06-15");
    }

    @Test
    void theScheduledEntryPointDoesTheSameWork() {
        Instant now = Instant.parse("2026-06-15T06:00:00Z");

        new GrantExpiryJob(grants, Clock.fixed(now, INDIA)).runNightly();

        verify(grants).expireGrantsWithDeadlineBefore(LocalDate.of(2026, 6, 15), now);
    }
}
