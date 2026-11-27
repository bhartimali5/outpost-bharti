package com.thub.outpost.grant.repository;

import com.thub.outpost.grant.entity.Grant;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Database access for grants. There is no class to write: Spring Data generates the implementation
 * at startup. JpaRepository already provides save, findById, findAll(Pageable), deleteById, count, ...
 *
 * JpaSpecificationExecutor adds findAll(Specification, Pageable): filtering by composable conditions,
 * used by the founder listing (see {@link GrantSpecifications}).
 *
 * The three methods below are "derived queries": Spring reads the method NAME and builds the SQL from it.
 * They back the admin status filter (see GrantStatus).
 */
public interface GrantRepository extends JpaRepository<Grant, UUID>, JpaSpecificationExecutor<Grant> {

    /** WHERE expired = true */
    Page<Grant> findByExpiredTrue(Pageable pageable);

    /** WHERE published = true AND expired = false */
    Page<Grant> findByPublishedTrueAndExpiredFalse(Pageable pageable);

    /** WHERE published = false AND expired = false */
    Page<Grant> findByPublishedFalseAndExpiredFalse(Pageable pageable);

    /**
     * Marks every not-yet-expired grant whose deadline is before {@code today} as expired, in ONE UPDATE statement,
     * and returns how many rows changed. A NULL deadline (rolling grant) never matches "deadline &lt; today".
     *
     * A bulk JPQL update goes straight to the database, skipping entity listeners, so the auditing listener does
     * not run: updatedAt is set here explicitly. @Transactional because an UPDATE needs a transaction, and the
     * caller (the scheduled job) has none of its own.
     */
    @Modifying
    @Transactional
    @Query("update Grant g set g.expired = true, g.updatedAt = :now where g.expired = false and g.deadline < :today")
    int expireGrantsWithDeadlineBefore(@Param("today") LocalDate today, @Param("now") Instant now);
}
