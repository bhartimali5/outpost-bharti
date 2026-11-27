package com.thub.outpost.grant.dto;

import com.thub.outpost.grant.entity.Grant;
import com.thub.outpost.grant.entity.GrantType;
import com.thub.outpost.lookup.dto.LookupItem;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * One card in the founder grants listing: only what the card shows, nothing internal
 * (no published/expired flags, no timestamps, no long text fields).
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
                LookupItem.ofStages(grant.getStages()),
                LookupItem.ofSectors(grant.getSectors()));
    }
}
