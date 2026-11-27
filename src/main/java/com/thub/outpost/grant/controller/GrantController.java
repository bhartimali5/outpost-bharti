package com.thub.outpost.grant.controller;

import com.thub.outpost.common.OpenApiConfig;
import com.thub.outpost.common.PageResponse;
import com.thub.outpost.grant.dto.GrantCardResponse;
import com.thub.outpost.grant.dto.GrantFilter;
import com.thub.outpost.grant.service.GrantQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Founder-facing grants. Requires a signed-in user (login arrives with OP-017).
 * Only published, non-expired grants are ever returned.
 */
@RestController
@RequestMapping("/api/v1/grants")
@Tag(name = OpenApiConfig.GRANTS)
public class GrantController {

    private final GrantQueryService service;

    public GrantController(GrantQueryService service) {
        this.service = service;
    }

    /**
     * {@code @ParameterObject} tells springdoc to show each field of the filter record and each paging value as
     * its own query parameter in Swagger UI (otherwise they appear as one opaque object).
     * The default sort is deadline (soonest first, rolling grants last), then name.
     */
    @GetMapping
    @Operation(summary = "Browse published grants",
            description = """
                    Published, non-expired grants only. All filters are optional and combine with AND.
                    q: keyword search over name, issuing body and summary (case-insensitive, partial words match).
                    amountBand is based on the grant's maximum amount: UNDER_10L (< 10 lakh), 10L_50L, 50L_1CR, OVER_1CR (>= 1 crore).
                    deadline: OPEN_NOW (rolling or not yet passed), CLOSING_THIS_MONTH, ROLLING (no deadline).
                    Paging: size is at most 50 (400 above that). sort may be deadline, name or createdAt, for example
                    sort=name,desc (anything else is a 400). The default is deadline soonest first, then name.
                    Rolling grants (no deadline) always come last.
                    A search with no matches is still a 200 with an empty content list, never a 404.""")
    public PageResponse<GrantCardResponse> list(
            @ParameterObject GrantFilter filter,
            @ParameterObject @PageableDefault(size = 20, sort = {"deadline", "name"}) Pageable pageable) {
        return service.list(filter, pageable);
    }
}
