package com.thub.outpost.grant.repository;

import com.thub.outpost.grant.entity.Grant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Database access for grants. There is no class to write: Spring Data generates the implementation
 * at startup. JpaRepository already provides save, findById, findAll(Pageable), deleteById, count, ...
 *
 * The methods below are "derived queries": Spring reads the method NAME and builds the SQL from it.
 * They back the admin status filter (see GrantStatus). Filtering for founders is added in OP-011.
 */
public interface GrantRepository extends JpaRepository<Grant, UUID> {

    /** WHERE expired = true */
    Page<Grant> findByExpiredTrue(Pageable pageable);

    /** WHERE published = true AND expired = false */
    Page<Grant> findByPublishedTrueAndExpiredFalse(Pageable pageable);

    /** WHERE published = false AND expired = false */
    Page<Grant> findByPublishedFalseAndExpiredFalse(Pageable pageable);
}
