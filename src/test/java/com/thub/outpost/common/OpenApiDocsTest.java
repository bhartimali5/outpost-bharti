package com.thub.outpost.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.thub.outpost.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Smoke test for OP-006: the generated OpenAPI document is public and carries our configuration.
 * The full application starts (including the security filters), so this also proves
 * that /v3/api-docs is reachable without a token.
 */
class OpenApiDocsTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void apiDocs_isPublicAndDescribesTheApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("Outpost API"))
                .andExpect(jsonPath("$.info.version").value("v1"));
    }

    @Test
    void apiDocs_declaresJwtBearerSchemeAndFeatureTags() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
                .andExpect(jsonPath("$.tags[0].name").value("Grants"))
                .andExpect(jsonPath("$.tags[1].name").value("Admin – Grants"));
    }
}
