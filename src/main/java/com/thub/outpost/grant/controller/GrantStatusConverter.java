package com.thub.outpost.grant.controller;

import com.thub.outpost.grant.entity.GrantStatus;
import java.util.Locale;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Lets the API accept ?status=published (lowercase, as in the story) for the GrantStatus enum.
 * Spring's built-in String-to-enum conversion is case-sensitive and would only accept PUBLISHED.
 *
 * Any Converter bean is picked up automatically by Spring MVC. An unknown value throws
 * IllegalArgumentException, which Spring MVC reports as a 400.
 */
@Component
class GrantStatusConverter implements Converter<String, GrantStatus> {

    @Override
    public GrantStatus convert(String source) {
        return GrantStatus.valueOf(source.trim().toUpperCase(Locale.ROOT));
    }
}
