package com.thub.outpost.grant.dto;

import com.thub.outpost.grant.entity.GrantType;
import com.thub.outpost.grant.entity.IssuingBodyType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.hibernate.validator.constraints.URL;

/**
 * Body of POST (create) and PUT (full replace) for admin grants.
 *
 * A DTO (data transfer object) is the shape of the JSON on the wire. It is separate from the Grant entity
 * on purpose: the API must not expose or accept database fields such as id, published, expired or the
 * timestamps. Validation annotations are checked when the controller parameter is marked @Valid; a failure
 * becomes a 400 with errors[] via GlobalExceptionHandler.
 *
 * Optional fields can be null. stageIds/sectorIds may be omitted, which means "none".
 */
public record GrantRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 255) String issuingBody,
        @NotNull IssuingBodyType issuingBodyType,
        @NotNull GrantType type,
        @DecimalMin("0") BigDecimal amountMin,
        @DecimalMin("0") BigDecimal amountMax,
        @Size(max = 255) String amountDisplay,
        LocalDate deadline,
        @NotBlank String summary,
        String eligibility,
        String whatIsFunded,
        String howToApply,
        @NotBlank @Size(max = 2048) @URL(regexp = "^https?://.+") String sourceUrl,
        List<UUID> stageIds,
        List<UUID> sectorIds) {

    /** Compact constructor: runs on every instance. Turns a missing list into an empty one. */
    public GrantRequest {
        stageIds = stageIds == null ? List.of() : List.copyOf(stageIds);
        sectorIds = sectorIds == null ? List.of() : List.copyOf(sectorIds);
    }
}
