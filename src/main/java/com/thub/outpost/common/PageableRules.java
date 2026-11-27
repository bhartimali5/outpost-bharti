package com.thub.outpost.common;

import com.thub.outpost.common.error.FieldIssue;
import com.thub.outpost.common.error.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Rules every list endpoint applies to the paging parameters a client sends (?page=&size=&sort=).
 *
 * <ul>
 *   <li>size is capped at {@link #MAX_PAGE_SIZE}. Above it the request is rejected with a 400, not silently
 *       shrunk, so a client never gets fewer rows than it asked for without knowing.</li>
 *   <li>sort may only use a whitelist of fields. Spring Data passes the client's sort text straight to the
 *       query, so without a whitelist a client could sort by any attribute, including relations such as
 *       "stages.name", or cause a 500 with an unknown name.</li>
 * </ul>
 *
 * NULL ordering is not handled here: Spring Data cannot apply per-sort "nulls last" to Specification queries.
 * It is set globally instead (hibernate.order_by.default_null_ordering = last in application.yml).
 *
 * Reused by every module's list endpoints (grants now; challenges, mentors and others later).
 */
public final class PageableRules {

    public static final int MAX_PAGE_SIZE = 50;

    private PageableRules() {
    }

    /**
     * Checks the request; returns it unchanged when it is acceptable.
     *
     * @param allowedSortFields entity fields a client may sort by
     * @throws ValidationException (400) listing every problem found
     */
    public static Pageable validated(Pageable pageable, Set<String> allowedSortFields) {
        List<FieldIssue> problems = new ArrayList<>();
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            problems.add(new FieldIssue("size", "must be at most " + MAX_PAGE_SIZE));
        }
        for (Sort.Order order : pageable.getSort()) {
            if (!allowedSortFields.contains(order.getProperty())) {
                problems.add(new FieldIssue("sort", "cannot sort by '" + order.getProperty()
                        + "'; allowed: " + String.join(", ", new TreeSet<>(allowedSortFields))));
            }
        }
        if (!problems.isEmpty()) {
            throw new ValidationException("INVALID_PAGE_REQUEST", "Invalid paging or sorting parameters", problems);
        }
        return pageable;
    }
}
