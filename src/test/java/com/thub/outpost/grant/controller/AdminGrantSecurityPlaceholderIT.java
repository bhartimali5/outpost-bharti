package com.thub.outpost.grant.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.thub.outpost.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/**
 * TRIPWIRE for OP-027. Until admin login exists, the story says admin endpoints are open, so this test
 * asserts that an anonymous caller gets in. It is meant to FAIL the day security is switched on.
 *
 * When OP-027 makes /api/v1/admin/** require the ADMIN role, this test breaks. That is the signal to:
 *   1. delete the permitAll rule for /api/v1/admin/** in SecurityConfig (it is marked TODO(OP-027)), and
 *   2. change this test to expect 401 for anonymous and 403 for a founder token (TestAuth).
 */
class AdminGrantSecurityPlaceholderIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void adminEndpoints_areOpenToAnonymousCallers_untilOp027() throws Exception {
        mockMvc.perform(get("/api/v1/admin/grants")).andExpect(status().isOk());
    }

    @Test
    void otherEndpoints_stillRequireAuthentication() throws Exception {
        // Guards the guard: opening the admin path must not have opened everything.
        mockMvc.perform(get("/api/v1/grants")).andExpect(status().isForbidden());
    }
}
