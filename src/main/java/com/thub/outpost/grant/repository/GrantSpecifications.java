package com.thub.outpost.grant.repository;

import com.thub.outpost.grant.dto.AmountBand;
import com.thub.outpost.grant.dto.DeadlineFilter;
import com.thub.outpost.grant.dto.GrantFilter;
import com.thub.outpost.grant.entity.Grant;
import com.thub.outpost.grant.entity.GrantType;
import com.thub.outpost.grant.entity.IssuingBodyType;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.data.jpa.domain.Specification;

/**
 * The founder listing's query rules, built from small reusable pieces.
 *
 * A {@link Specification} is one WHERE condition as an object. Each filter below is its own small
 * Specification, and {@link #forFilter} ANDs together the ones the caller asked for. Spring Data then turns the
 * result into SQL, so there is no string building and no chain of if/else over query text.
 *
 * Attribute names ("type", "stages", ...) are strings that must match the Grant entity's field names.
 * A typo would only fail at run time, which is why every filter has a test.
 */
public final class GrantSpecifications {

    private GrantSpecifications() {
    }

    /**
     * The whole founder-facing query: the default view (published and not expired) plus whichever filters are set.
     *
     * @param today the current date from the application Clock, used by the deadline filters
     */
    public static Specification<Grant> forFilter(GrantFilter filter, LocalDate today) {
        // Specification.allOf ignores null entries, so "filter not set" is simply null here.
        return Specification.allOf(
                isVisibleToFounders(),
                whenSet(blankToNull(filter.q()), GrantSpecifications::matchesKeyword),
                whenSet(filter.issuingBodyType(), GrantSpecifications::hasIssuingBodyType),
                whenSet(filter.type(), GrantSpecifications::hasType),
                whenSet(filter.stageId(), GrantSpecifications::hasStage),
                whenSet(filter.sectorId(), GrantSpecifications::hasSector),
                whenSet(filter.amountBand(), GrantSpecifications::inAmountBand),
                whenSet(filter.deadline(), deadline -> matchesDeadline(deadline, today)));
    }

    /** The default view: published and not expired. Drafts and expired grants never reach founders. */
    public static Specification<Grant> isVisibleToFounders() {
        return (root, query, cb) -> cb.and(
                cb.isTrue(root.get("published")),
                cb.isFalse(root.get("expired")));
    }

    /**
     * Keyword search: the text appears in the name, the issuing body or the summary, ignoring case.
     * Equivalent to PostgreSQL's {@code ILIKE '%text%'} on those three columns (written as lower() + LIKE, which
     * is the portable form in JPA).
     *
     * LIMITS: a leading wildcard cannot use a normal index, so Postgres scans the table. That is fine for a
     * directory of a few thousand grants. When it grows, switch to a pg_trgm GIN index or full-text search.
     */
    public static Specification<Grant> matchesKeyword(String text) {
        String pattern = "%" + escapeLikeWildcards(text.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern, LIKE_ESCAPE),
                cb.like(cb.lower(root.get("issuingBody")), pattern, LIKE_ESCAPE),
                cb.like(cb.lower(root.get("summary")), pattern, LIKE_ESCAPE));
    }

    public static Specification<Grant> hasIssuingBodyType(IssuingBodyType issuingBodyType) {
        return (root, query, cb) -> cb.equal(root.get("issuingBodyType"), issuingBodyType);
    }

    public static Specification<Grant> hasType(GrantType type) {
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    /** Grants open to the given stage. Joins the many-to-many link; see the note on distinct below. */
    public static Specification<Grant> hasStage(UUID stageId) {
        return (root, query, cb) -> {
            // Joining a collection repeats a grant once per matching link. DISTINCT keeps one row per grant,
            // and Spring Data also counts distinct grants for the page totals.
            query.distinct(true);
            return cb.equal(root.join("stages").get("id"), stageId);
        };
    }

    public static Specification<Grant> hasSector(UUID sectorId) {
        return (root, query, cb) -> {
            query.distinct(true);
            return cb.equal(root.join("sectors").get("id"), sectorId);
        };
    }

    /** Placed by amountMax (see {@link AmountBand}). A grant with no amountMax matches no band. */
    public static Specification<Grant> inAmountBand(AmountBand band) {
        return (root, query, cb) -> {
            List<Predicate> limits = new ArrayList<>();
            if (band.minInclusive() != null) {
                limits.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("amountMax"), band.minInclusive()));
            }
            if (band.maxExclusive() != null) {
                limits.add(cb.lessThan(root.<BigDecimal>get("amountMax"), band.maxExclusive()));
            }
            // A comparison against a NULL amountMax is never true, so grants without an amount drop out.
            return cb.and(limits.toArray(new Predicate[0]));
        };
    }

    /** The deadline buckets (see {@link DeadlineFilter}). A null deadline means a rolling grant. */
    public static Specification<Grant> matchesDeadline(DeadlineFilter filter, LocalDate today) {
        return (root, query, cb) -> {
            var deadline = root.<LocalDate>get("deadline");
            return switch (filter) {
                case ROLLING -> cb.isNull(deadline);
                case OPEN_NOW -> cb.or(cb.isNull(deadline), cb.greaterThanOrEqualTo(deadline, today));
                case CLOSING_THIS_MONTH -> cb.between(deadline, today, today.with(TemporalAdjusters.lastDayOfMonth()));
            };
        };
    }

    /** Builds the Specification only when the filter value was supplied; otherwise null (= no restriction). */
    private static <V> Specification<Grant> whenSet(V value, Function<V, Specification<Grant>> factory) {
        return value == null ? null : factory.apply(value);
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text;
    }

    private static final char LIKE_ESCAPE = '\\';

    /**
     * In a LIKE pattern, % and _ are wildcards. A user searching for "100%" means the literal percent sign, so
     * those characters (and the escape character itself) are escaped. Without this, "%" would match everything.
     */
    private static String escapeLikeWildcards(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
