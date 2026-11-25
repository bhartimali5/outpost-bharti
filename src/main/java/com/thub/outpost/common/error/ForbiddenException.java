package com.thub.outpost.common.error;

import org.springframework.http.HttpStatus;

/** 403. The caller is authenticated but not allowed to do this, e.g. EMAIL_NOT_VERIFIED. */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String code, String message) {
        super(HttpStatus.FORBIDDEN, code, message);
    }
}
