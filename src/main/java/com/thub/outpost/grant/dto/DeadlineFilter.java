package com.thub.outpost.grant.dto;

/**
 * Deadline buckets in the founder filter. "Today" comes from the application Clock (India time).
 *
 * <pre>
 *   OPEN_NOW            no deadline (rolling), or a deadline today or later
 *   CLOSING_THIS_MONTH  a deadline from today up to and including the last day of this month
 *   ROLLING             no deadline
 * </pre>
 */
public enum DeadlineFilter {
    OPEN_NOW,
    CLOSING_THIS_MONTH,
    ROLLING
}
