package com.thub.outpost.grant.dto;

import com.thub.outpost.grant.entity.Grant;
import com.thub.outpost.grant.entity.GrantType;
import com.thub.outpost.lookup.dto.LookupItem;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The founder-facing detail page of one grant (spec p.5 "Detail page per entry"): everything a founder needs to
 * decide whether to apply, plus {@code sourceUrl}, the link to the real application.
 *
 * Unlike the admin {@link GrantResponse}, it leaves out internal fields: the published flag (a founder only ever
 * sees published grants), the raw amountMin/amountMax (founders see T-Hub's amountDisplay text) and timestamps.
 *
 * Like the listing card, a rolling grant has {@code deadline = null} and {@code rolling = true}.
 * {@link #from(Grant)} reads lazy stages and sectors, so call it inside a transaction.
 */
public record GrantDetailResponse(
        UUID id,
        String name,
        String issuingBody,
        GrantType type,
        String amountDisplay,
        LocalDate deadline,
        boolean rolling,
        String summary,
        String eligibility,
        String whatIsFunded,
        String howToApply,
        String sourceUrl,
        List<LookupItem> stages,
        List<LookupItem> sectors,
        boolean expired) {

    public static GrantDetailResponse from(Grant grant) {
        return new GrantDetailResponse(
                grant.getId(),
                grant.getName(),
                grant.getIssuingBody(),
                grant.getType(),
                grant.getAmountDisplay(),
                grant.getDeadline(),
                grant.getDeadline() == null,
                grant.getSummary(),
                grant.getEligibility(),
                grant.getWhatIsFunded(),
                grant.getHowToApply(),
                grant.getSourceUrl(),
                LookupItem.ofStages(grant.getStages()),
                LookupItem.ofSectors(grant.getSectors()),
                grant.isExpired());
    }
}
