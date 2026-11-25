package com.thub.outpost.common.error;

import org.springframework.http.HttpStatus;

/**
 * Base class for every expected, business-level error in the API.
 *
 * Each exception carries the HTTP status it maps to and a stable, machine-readable {@code code}
 * (for example GRANT_NOT_FOUND). Clients switch on the code; the human message may change.
 *
 * It extends RuntimeException (unchecked) so service code doesn't need "throws" clauses,
 * and Spring rolls back transactions on it by default.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
