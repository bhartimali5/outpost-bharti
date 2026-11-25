package com.thub.outpost.common.error;

import org.springframework.http.HttpStatus;

/** 409. The request is valid but clashes with current state, e.g. EMAIL_TAKEN. */
public class ConflictException extends ApiException {

    public ConflictException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}
