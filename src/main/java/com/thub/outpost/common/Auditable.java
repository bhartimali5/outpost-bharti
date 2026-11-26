package com.thub.outpost.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Base class that gives an entity created_at / updated_at columns, filled in automatically.
 * Entities with those two columns extend it (Grant now; Founder and the rest later).
 *
 * {@code @MappedSuperclass}: not an entity itself; its fields become columns of each subclass's table.
 * {@code AuditingEntityListener} sets the timestamps on insert/update; it only works because
 * {@link JpaAuditingConfig} enables auditing. Instant is UTC, matching the API's "UTC ISO-8601" rule.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
