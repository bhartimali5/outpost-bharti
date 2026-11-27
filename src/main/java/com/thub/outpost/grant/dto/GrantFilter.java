package com.thub.outpost.grant.dto;

import com.thub.outpost.grant.entity.GrantType;
import com.thub.outpost.grant.entity.IssuingBodyType;
import java.util.UUID;

/**
 * The optional filters of the founder grants listing, bound from the query string:
 * {@code ?issuingBodyType=CENTRAL&type=GRANT&stageId=...&sectorId=...&amountBand=10L_50L&deadline=OPEN_NOW}.
 * Every field is optional (null = "do not filter on this"); the filters that are given are combined with AND.
 */
public record GrantFilter(
        IssuingBodyType issuingBodyType,
        GrantType type,
        UUID stageId,
        UUID sectorId,
        AmountBand amountBand,
        DeadlineFilter deadline) {
}
