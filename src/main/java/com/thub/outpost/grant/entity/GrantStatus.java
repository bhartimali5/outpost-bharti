package com.thub.outpost.grant.entity;

/**
 * The lifecycle state of a grant as admins see it. It is DERIVED from the two stored flags
 * (published, expired), not stored itself:
 *
 * <ul>
 *   <li>EXPIRED: expired is true (wins over published)</li>
 *   <li>PUBLISHED: published and not expired</li>
 *   <li>DRAFT: neither</li>
 * </ul>
 */
public enum GrantStatus {
    DRAFT,
    PUBLISHED,
    EXPIRED
}
