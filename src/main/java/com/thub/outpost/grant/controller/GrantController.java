package com.thub.outpost.grant.controller;

import com.thub.outpost.common.OpenApiConfig;
import com.thub.outpost.common.PageResponse;
import com.thub.outpost.grant.dto.GrantCardResponse;
import com.thub.outpost.grant.dto.GrantDetailResponse;
import com.thub.outpost.grant.dto.GrantFilter;
import com.thub.outpost.grant.service.GrantQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Founder-facing grants. Requires a signed-in user (login arrives with OP-017).
 * Founders only ever see published grants; drafts do not exist as far as this API is concerned.
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
                    includeExpired: expired grants are hidden by default; true includes them, marked expired: true.
                    Paging: size is at most 50 (400 above that). sort may be deadline, name or createdAt, for example
                    sort=name,desc (anything else is a 400). The default is deadline soonest first, then name.
                    Rolling grants (no deadline) always come last.""")
    // The empty-state contract, stated where clients read it: no matches is a normal, successful answer.
    @ApiResponse(responseCode = "200", description = """
            One page of grant cards. EMPTY-STATE CONTRACT: when nothing matches (no grants at all, or no grants for \
            these filters) the response is still 200, with content = [], totalElements = 0 and totalPages = 0. \
            It is never a 404. Clients show their "no results" message when content is empty.""")
    @ApiResponse(responseCode = "400", description = "An unknown filter value, size above 50 or a sort field that is "
            + "not allowed. errors[] says which.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public PageResponse<GrantCardResponse> list(
            @ParameterObject GrantFilter filter,
            @ParameterObject @PageableDefault(size = 20, sort = {"deadline", "name"}) Pageable pageable) {
        return service.list(filter, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Grant detail page",
            description = "Everything a founder needs to decide, plus sourceUrl: the link to the real application. "
                    + "Expired grants are still returned, with expired: true.")
    @ApiResponse(responseCode = "200", description = "The grant.")
    @ApiResponse(responseCode = "404", description = "GRANT_NOT_FOUND: no published grant has this id. "
            + "Drafts get the same 404 as ids that do not exist.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "400", description = "The id is not a valid UUID.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public GrantDetailResponse get(@PathVariable UUID id) {
        return service.get(id);
    }
}
