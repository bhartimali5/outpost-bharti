package com.thub.outpost.grant.service;

import com.thub.outpost.common.PageableRules;
import java.util.Set;
import org.springframework.data.domain.Pageable;

/**
 * The paging rules for grant lists (founder and admin): which fields a client may sort by.
 * Rolling grants (NULL deadline) sort last in both directions thanks to the global Hibernate setting
 * hibernate.order_by.default_null_ordering = last.
 */
final class GrantPaging {

    /** Entity field names a client may sort by. Anything else is a 400. */
    private static final Set<String> SORTABLE = Set.of("deadline", "name", "createdAt");

    private GrantPaging() {
    }

    static Pageable validated(Pageable pageable) {
        return PageableRules.validated(pageable, SORTABLE);
    }
}
