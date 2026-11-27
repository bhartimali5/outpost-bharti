package com.thub.outpost.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on @Scheduled methods (the nightly jobs: grant expiry now, challenge closing and the spotlight later).
 *
 * In its own config class, not on the main application class, so slice tests that load the main class do not
 * start schedulers. Each job's cron expression lives in application.yml under outpost.jobs, and the test
 * profile switches the jobs off ("-"); tests call the job methods directly instead.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
