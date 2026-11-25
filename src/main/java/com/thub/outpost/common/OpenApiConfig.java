package com.thub.outpost.common;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Describes the API in the generated OpenAPI document (/v3/api-docs) and Swagger UI.
 * springdoc builds the endpoint list from the controllers; this bean adds the general information.
 */
@Configuration
public class OpenApiConfig {

    static final String BEARER_SCHEME = "bearerAuth";

    /** Tag names, shared so controllers use @Tag(name = OpenApiConfig.GRANTS) and can't mistype them. */
    public static final String GRANTS = "Grants";
    public static final String ADMIN_GRANTS = "Admin – Grants";

    @Bean
    OpenAPI outpostOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Outpost API")
                        .version("v1")
                        .description("Backend for Outpost, T-Hub's curated directory for startup founders: "
                                + "grants, corporate challenges and mentors."))
                // Declares HOW to authenticate: an "Authorization: Bearer <JWT>" header.
                // This is what makes the "Authorize" button appear in Swagger UI (tokens arrive in OP-017).
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                // Applies the scheme to every endpoint, so Swagger UI sends the token on all calls.
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                // Declaring tags here controls their ORDER and DESCRIPTION in Swagger UI.
                // A controller joins a group with @Tag(name = "Grants") on the class.
                // Add one entry per feature as its controllers are built.
                .tags(List.of(
                        new Tag().name(GRANTS).description("Founder-facing grants and schemes directory"),
                        new Tag().name(ADMIN_GRANTS).description("T-Hub staff: manage grants (ADMIN role)")));
    }
}
