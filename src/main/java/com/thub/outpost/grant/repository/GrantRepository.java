package com.thub.outpost.grant.repository;

import com.thub.outpost.grant.entity.Grant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

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
}
