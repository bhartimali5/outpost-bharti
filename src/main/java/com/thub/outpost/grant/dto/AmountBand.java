package com.thub.outpost.grant.dto;

import com.fasterxml.jackson.annotation.JsonValue;
import java.math.BigDecimal;
import java.util.Arrays;

/**
 * The amount bands offered in the founder filter, in Indian units (1 lakh = 100,000; 1 crore = 10,000,000).
 *
 * A grant is placed in a band by its {@code amountMax}, the most a founder can receive. A grant with no
 * amountMax is in no band, so it never matches an amountBand filter.
 *
 * <pre>
 *   UNDER_10L   amountMax &lt; 10 lakh
 *   10L_50L     10 lakh  &lt;= amountMax &lt; 50 lakh
 *   50L_1CR     50 lakh  &lt;= amountMax &lt; 1 crore
 *   OVER_1CR    amountMax &gt;= 1 crore
 * </pre>
 *
 * Java names cannot start with a digit, so the constants have longer names and the API uses {@link #value()}.
 */
public enum AmountBand {

    UNDER_10L("UNDER_10L", null, bd(1_000_000)),
    FROM_10L_TO_50L("10L_50L", bd(1_000_000), bd(5_000_000)),
    FROM_50L_TO_1CR("50L_1CR", bd(5_000_000), bd(10_000_000)),
    OVER_1CR("OVER_1CR", bd(10_000_000), null);

    private final String value;
    private final BigDecimal minInclusive;
    private final BigDecimal maxExclusive;

    AmountBand(String value, BigDecimal minInclusive, BigDecimal maxExclusive) {
        this.value = value;
        this.minInclusive = minInclusive;
        this.maxExclusive = maxExclusive;
    }

    /** The text used in the API (query parameter and OpenAPI document). */
    @JsonValue
    public String value() {
        return value;
    }

    /** Lower limit (included), or null when the band has no lower limit. */
    public BigDecimal minInclusive() {
        return minInclusive;
    }

    /** Upper limit (NOT included), or null when the band has no upper limit. */
    public BigDecimal maxExclusive() {
        return maxExclusive;
    }

    public static AmountBand fromValue(String text) {
        String wanted = text.trim();
        return Arrays.stream(values())
                .filter(band -> band.value.equalsIgnoreCase(wanted))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown amount band: " + text));
    }

    private static BigDecimal bd(long amount) {
        return BigDecimal.valueOf(amount);
    }
}
