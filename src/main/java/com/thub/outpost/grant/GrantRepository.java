package com.thub.outpost.grant;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Database access for grants. There is no class to write: Spring Data generates the implementation
 * at startup. JpaRepository already provides save, findById, findAll(Pageable), deleteById, count, ...
 * Filtering support is added in OP-011.
 */
public interface GrantRepository extends JpaRepository<Grant, UUID> {
}
