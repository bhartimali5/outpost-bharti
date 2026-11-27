package com.thub.outpost.grant.dto;

import com.thub.outpost.grant.entity.GrantType;
import com.thub.outpost.grant.entity.IssuingBodyType;
import java.util.UUID;

/**
 * The optional filters of the founder grants listing, bound from the query string:
 * {@code ?q=women&issuingBodyType=CENTRAL&type=GRANT&stageId=...&sectorId=...&amountBand=10L_50L&deadline=OPEN_NOW}.
 * Every field is optional (null = "do not filter on this"); the filters that are given are combined with AND.
 *
 * {@code q} is a keyword search: it matches grants whose name, issuing body or summary contains the text,
 * ignoring case. A blank q is treated as not given.
 */
public record GrantFilter(
        String q,
        IssuingBodyType issuingBodyType,
        GrantType type,
        UUID stageId,
        UUID sectorId,
        AmountBand amountBand,
        DeadlineFilter deadline) {
}
