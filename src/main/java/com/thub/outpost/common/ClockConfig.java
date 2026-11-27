package com.thub.outpost.common;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The one source of "now" for business rules. Business code takes a {@link Clock} instead of calling
 * LocalDate.now(), so tests can supply a fixed clock and the time zone is decided in one place.
 *
 * Deadlines are plain dates and the audience is in India, so "today" means today in India time. (Stored
 * timestamps are still UTC; this only affects what counts as today's date.)
 */
@Configuration
public class ClockConfig {

    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    @Bean
    Clock clock() {
        return Clock.system(BUSINESS_ZONE);
    }
}
