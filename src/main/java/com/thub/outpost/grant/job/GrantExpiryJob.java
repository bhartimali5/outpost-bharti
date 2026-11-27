package com.thub.outpost.grant.job;

import com.thub.outpost.common.ClockConfig;
import com.thub.outpost.grant.repository.GrantRepository;
import java.time.Clock;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Nightly job: marks grants whose deadline has passed as expired, so they drop off the founder listing
 * without an admin clicking anything (spec p.5 "Expired entries drop off the default view").
 *
 * <ul>
 *   <li>Expires grants with deadline &lt; today. A grant whose deadline is today stays open all of today.</li>
 *   <li>Rolling grants (no deadline) never expire.</li>
 *   <li>Idempotent: already-expired grants are left alone, so running it twice does no harm (the second run
 *       expires 0). That matters if the job is re-run by hand or two instances start at once.</li>
 *   <li>Drafts are expired too; expiry is about the deadline, not visibility.</li>
 * </ul>
 *
 * "Today" comes from the injected {@link Clock} (India time), never LocalDate.now(), so tests can fix the date.
 */
@Component
public class GrantExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(GrantExpiryJob.class);

    private final GrantRepository grants;
    private final Clock clock;

    public GrantExpiryJob(GrantRepository grants, Clock clock) {
        this.grants = grants;
        this.clock = clock;
    }

    /**
     * Entry point for the scheduler. The cron comes from configuration (outpost.jobs.grant-expiry.cron), and the
     * zone makes "00:05" mean 00:05 in India, not on the server's clock.
     */
    @Scheduled(cron = "${outpost.jobs.grant-expiry.cron}", zone = ClockConfig.BUSINESS_ZONE_ID)
    void runNightly() {
        expireOverdueGrants();
    }

    /** Does the work; public so it can be called directly from tests (or by an admin action later). */
    public int expireOverdueGrants() {
        LocalDate today = LocalDate.now(clock);
        int expired = grants.expireGrantsWithDeadlineBefore(today, clock.instant());
        log.info("Grant expiry job: expired {} grant(s) with a deadline before {}", expired, today);
        return expired;
    }
}
