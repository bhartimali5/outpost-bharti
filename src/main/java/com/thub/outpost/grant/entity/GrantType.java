package com.thub.outpost.grant.entity;

/** The kind of funding. Stored as its name (VARCHAR) via @Enumerated(STRING); never use ordinals. */
public enum GrantType {
    GRANT,
    SUBSIDY,
    SCHEME,
    INCENTIVE
}
