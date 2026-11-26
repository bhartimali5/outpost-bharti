package com.thub.outpost.common.error;

/** One problem with one request field. Serialized as {"field": "...", "message": "..."} inside errors[]. */
public record FieldIssue(String field, String message) {
}
