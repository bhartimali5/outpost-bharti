package com.thub.outpost.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.thub.outpost.common.error.FieldIssue;
import com.thub.outpost.common.error.ValidationException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** A plain unit test: no Spring, no database, so it runs in milliseconds. */
class PageableRulesTest {

    private static final Set<String> ALLOWED = Set.of("deadline", "name", "createdAt");

    @Test
    void acceptsTheMaximumPageSize_andReturnsTheRequestUnchanged() {
        Pageable requested = PageRequest.of(2, 50, Sort.by(Sort.Order.desc("deadline"), Sort.Order.asc("name")));

        assertThat(PageableRules.validated(requested, ALLOWED)).isEqualTo(requested);
    }

    @Test
    void rejectsAPageSizeAboveTheMaximum() {
        assertThatThrownBy(() -> PageableRules.validated(PageRequest.of(0, 51), ALLOWED))
                .isInstanceOfSatisfying(ValidationException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("INVALID_PAGE_REQUEST");
                    assertThat(e.getErrors()).extracting(FieldIssue::field).containsExactly("size");
                });
    }

    @Test
    void rejectsASortFieldThatIsNotWhitelisted() {
        assertThatThrownBy(() -> PageableRules.validated(PageRequest.of(0, 20, Sort.by("evil")), ALLOWED))
                .isInstanceOfSatisfying(ValidationException.class, e -> {
                    assertThat(e.getErrors()).extracting(FieldIssue::field).containsExactly("sort");
                    assertThat(e.getErrors().get(0).message()).contains("evil").contains("createdAt, deadline, name");
                });
    }

    @Test
    void rejectsRelationPathsToo() {
        assertThatThrownBy(() -> PageableRules.validated(PageRequest.of(0, 20, Sort.by("stages.name")), ALLOWED))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void reportsEveryProblemTogether() {
        assertThatThrownBy(() -> PageableRules.validated(PageRequest.of(0, 500, Sort.by("evil")), ALLOWED))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getErrors()).extracting(FieldIssue::field).containsExactlyInAnyOrder("size", "sort"));
    }
}
