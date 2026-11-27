package com.thub.outpost.grant.dto;

import com.thub.outpost.grant.entity.Grant;
import com.thub.outpost.grant.entity.GrantStatus;
import com.thub.outpost.grant.entity.GrantType;
import com.thub.outpost.grant.entity.IssuingBodyType;
import com.thub.outpost.lookup.dto.LookupItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * What the admin API returns for a grant. Never return the entity itself: it would expose internals,
 * and serializing its lazy collections outside a transaction would fail.
 *
 * {@link #from(Grant)} reads the lazy stages/sectors, so it must be called while a transaction is open
 * (that is, from inside a @Transactional service method).
 */
public record GrantResponse(
        UUID id,
        String name,
        String issuingBody,
        IssuingBodyType issuingBodyType,
        GrantType type,
        BigDecimal amountMin,
        BigDecimal amountMax,
        String amountDisplay,
        LocalDate deadline,
        String summary,
        String eligibility,
        String whatIsFunded,
        String howToApply,
        String sourceUrl,
        boolean published,
        boolean expired,
        GrantStatus status,
        List<LookupItem> stages,
        List<LookupItem> sectors,
        Instant createdAt,
        Instant updatedAt) {

    public static GrantResponse from(Grant grant) {
        return new GrantResponse(
                grant.getId(),
                grant.getName(),
                grant.getIssuingBody(),
                grant.getIssuingBodyType(),
                grant.getType(),
                grant.getAmountMin(),
                grant.getAmountMax(),
                grant.getAmountDisplay(),
                grant.getDeadline(),
                grant.getSummary(),
                grant.getEligibility(),
                grant.getWhatIsFunded(),
                grant.getHowToApply(),
                grant.getSourceUrl(),
                grant.isPublished(),
                grant.isExpired(),
                grant.getStatus(),
                // A Set has no order; LookupItem sorts for a stable, predictable response.
                LookupItem.ofStages(grant.getStages()),
                LookupItem.ofSectors(grant.getSectors()),
                grant.getCreatedAt(),
                grant.getUpdatedAt());
    }
}
