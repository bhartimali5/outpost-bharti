package com.thub.outpost.lookup.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Startup stage (dropdown value set by T-Hub). A lookup row, not a Java enum, so admins can change
 * the list without a deploy (OP-025).
 *
 * equals/hashCode are based on the id only, written by hand on purpose: Lombok's @Data would include
 * every field, and entities in a HashSet then break when a field changes or a lazy proxy is involved.
 */
@Entity
@Table(name = "stage")
public class Stage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active = true;

    protected Stage() {
        // required by JPA
    }

    public Stage(String name, int sortOrder) {
        this.name = name;
        this.sortOrder = sortOrder;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        // instanceof (not getClass()) so Hibernate lazy proxies of Stage compare equal too.
        return o instanceof Stage other && getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        // Constant on purpose: the id is null until the row is saved, and a hash that changes
        // while the object sits in a HashSet would make it unfindable.
        return Stage.class.hashCode();
    }
}
