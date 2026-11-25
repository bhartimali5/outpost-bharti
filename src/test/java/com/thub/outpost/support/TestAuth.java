package com.thub.outpost.support;

/**
 * Mints JWTs for tests, so MockMvc requests can act as a founder or an admin:
 *
 * <pre>
 * mockMvc.perform(get("/api/v1/me").header("Authorization", TestAuth.bearer(TestAuth.founderToken())))
 * </pre>
 *
 * PLACEHOLDER: there is no token service yet. OP-017 adds JWT signing; fill these methods in then
 * (and OP-026 adds the variants: unverified, not onboarded, expired, tampered).
 * Until then they fail loudly, so nobody writes a test that silently runs unauthenticated.
 */
public final class TestAuth {

    private TestAuth() {
    }

    public static String founderToken() {
        throw notImplementedYet();
    }

    public static String adminToken() {
        throw notImplementedYet();
    }

    /** Value for the Authorization header. */
    public static String bearer(String token) {
        return "Bearer " + token;
    }

    private static UnsupportedOperationException notImplementedYet() {
        return new UnsupportedOperationException("TestAuth is filled in with OP-017 (JWT support)");
    }
}
