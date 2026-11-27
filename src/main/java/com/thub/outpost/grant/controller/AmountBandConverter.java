package com.thub.outpost.grant.controller;

import com.thub.outpost.grant.dto.AmountBand;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Lets the API accept ?amountBand=10L_50L. Spring's built-in String-to-enum conversion would look for a
 * constant named "10L_50L", which Java cannot have (names cannot start with a digit).
 * An unknown value throws IllegalArgumentException, which Spring MVC reports as a 400.
 */
@Component
class AmountBandConverter implements Converter<String, AmountBand> {

    @Override
    public AmountBand convert(String source) {
        return AmountBand.fromValue(source);
    }
}
