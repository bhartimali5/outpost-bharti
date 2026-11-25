package com.thub.outpost.common.error;

import org.springframework.http.HttpStatus;

/** 404. The code is derived from the entity name: new NotFoundException("Grant", id) gives GRANT_NOT_FOUND. */
public class NotFoundException extends ApiException {

    public NotFoundException(String entity, Object id) {
        super(HttpStatus.NOT_FOUND,
                entity.toUpperCase().replace(' ', '_') + "_NOT_FOUND",
                entity + " with id " + id + " was not found");
    }
}
