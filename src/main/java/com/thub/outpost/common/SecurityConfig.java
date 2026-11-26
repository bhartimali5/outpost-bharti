package com.thub.outpost.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * TEMPORARY security setup for OP-003. The real config (JWT filter, roles, CORS) arrives in OP-017.
 *
 * Without any SecurityFilterChain bean, Spring Boot locks every URL behind a login page.
 * Here we open only the health check and the API docs; everything else stays protected.
 */
@Configuration
public class SecurityConfig {

    // @Bean: the returned object is registered in the Spring container, and Spring Security uses it.
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // stateless API with tokens, no browser session/forms
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // TODO(OP-027): REMOVE this line. Admin endpoints are open ONLY because login does not
                        // exist yet (OP-010 story: "until OP-027 lands, admin endpoints are open").
                        // OP-027 must require role ADMIN here, and fix AdminGrantSecurityPlaceholderIT.
                        .requestMatchers("/api/v1/admin/**").permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }
}
