package com.thub.outpost;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point. {@code @SpringBootApplication} turns on three things:
 * component scanning of this package and everything below it, auto-configuration
 * based on the dependencies on the classpath, and this class acting as a configuration class.
 */
@SpringBootApplication
public class OutpostApplication {

    public static void main(String[] args) {
        SpringApplication.run(OutpostApplication.class, args);
    }
}
