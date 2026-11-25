package com.thub.outpost.grant;

/** Who issues the grant. Stored as its name (VARCHAR) via @Enumerated(STRING); never use ordinals. */
public enum IssuingBodyType {
    CENTRAL,
    STATE,
    OTHER
}
