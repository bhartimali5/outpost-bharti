package com.thub.outpost.grant.dto;

import com.thub.outpost.grant.entity.Grant;
import com.thub.outpost.grant.entity.GrantType;
import com.thub.outpost.lookup.dto.LookupItem;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * One card in the founder grants listing: only what the card shows, nothing internal
 * (no published flag, no timestamps, no long text fields).
 *
 * {@code expired} is false in the default view; it can only be true when the founder asked for includeExpired=true.
 *
 * A rolling grant has no deadline: {@code deadline} is null and {@code rolling} is true. Two typed fields,
 * instead of one field that is sometimes a date and sometimes the text "rolling", keep the contract clear
 * for clients.
 *
 * {@link #from(Grant)} reads lazy stages and sectors, so call it inside a transaction.
 */
public record GrantCardResponse(
        UUID id,
        String name,
        String issuingBody,
        GrantType type,
        String amountDisplay,
        LocalDate deadline,
        boolean rolling,
        boolean expired,
        List<LookupItem> stages,
        List<LookupItem> sectors) {

    public static GrantCardResponse from(Grant grant) {
        return new GrantCardResponse(
                grant.getId(),
                grant.getName(),
                grant.getIssuingBody(),
                grant.getType(),
                grant.getAmountDisplay(),
                grant.getDeadline(),
                grant.getDeadline() == null,
                grant.isExpired(),
                LookupItem.ofStages(grant.getStages()),
                LookupItem.ofSectors(grant.getSectors()));
    }
}
