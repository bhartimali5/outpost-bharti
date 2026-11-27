package com.thub.outpost.grant.service;

import com.thub.outpost.common.PageResponse;
import com.thub.outpost.grant.dto.GrantCardResponse;
import com.thub.outpost.grant.dto.GrantFilter;
import com.thub.outpost.grant.entity.Grant;
import com.thub.outpost.grant.repository.GrantRepository;
import com.thub.outpost.grant.repository.GrantSpecifications;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The founder side of grants: read-only queries over what T-Hub has published.
 * (The admin side, with writes, is {@link GrantAdminService}.)
 */
@Service
@Transactional(readOnly = true)
public class GrantQueryService {

    private final GrantRepository grants;
    private final Clock clock;

    public GrantQueryService(GrantRepository grants, Clock clock) {
        this.grants = grants;
        this.clock = clock;
    }

    /** Published, non-expired grants matching every filter that is set, one page at a time. */
    public PageResponse<GrantCardResponse> list(GrantFilter filter, Pageable pageable) {
        LocalDate today = LocalDate.now(clock); // from the injected Clock, never LocalDate.now() directly
        Pageable checked = GrantPaging.validated(pageable); // 400 for size > 50 or a sort field not whitelisted
        Page<Grant> page = grants.findAll(GrantSpecifications.forFilter(filter, today), checked);
        return PageResponse.from(page, GrantCardResponse::from);
    }
}
