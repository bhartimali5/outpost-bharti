package com.thub.outpost.common.error;

import org.springframework.http.HttpStatus;

/**
 * 400 for business-rule validation done in the service layer (e.g. "stage id does not exist").
 * Not to be confused with jakarta.validation.ValidationException, which Bean Validation uses.
 */
public class ValidationException extends ApiException {

    public ValidationException(String code, String message) {
        super(HttpStatus.BAD_REQUEST, code, message);
    }
}
