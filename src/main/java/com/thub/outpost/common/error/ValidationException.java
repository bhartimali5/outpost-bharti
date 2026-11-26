package com.thub.outpost.common.error;

import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * 400 for business-rule validation done in the service layer (e.g. "stage id does not exist").
 * Not to be confused with jakarta.validation.ValidationException, which Bean Validation uses.
 *
 * It can carry per-field problems; GlobalExceptionHandler puts them in the response's errors[],
 * the same shape used for @Valid failures.
 */
public class ValidationException extends ApiException {

    private final List<FieldIssue> errors;

    public ValidationException(String code, String message) {
        this(code, message, List.of());
    }

    public ValidationException(String code, String message, List<FieldIssue> errors) {
        super(HttpStatus.BAD_REQUEST, code, message);
        this.errors = List.copyOf(errors);
    }

    public List<FieldIssue> getErrors() {
        return errors;
    }
}
