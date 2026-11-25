package com.thub.outpost.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Turns on @CreatedDate / @LastModifiedDate handling (see {@link Auditable}).
 * Lives in its own config class, not on the main application class: putting it on the main class
 * would also load it in web-layer-only tests (@WebMvcTest), which have no JPA and would fail.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
