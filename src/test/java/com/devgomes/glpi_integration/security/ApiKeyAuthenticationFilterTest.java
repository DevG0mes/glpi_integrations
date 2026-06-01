package com.devgomes.glpi_integration.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiKeyAuthenticationFilterTest {

    @Test
    void publicPathsDoNotRequireKey() {
        assertTrue(ApiKeyAuthenticationFilter.isPublicPath("/"));
        assertTrue(ApiKeyAuthenticationFilter.isPublicPath("/app/index.html"));
        assertTrue(ApiKeyAuthenticationFilter.isPublicPath("/app/css/app.css"));
        assertTrue(ApiKeyAuthenticationFilter.isPublicPath("/actuator/health"));
        assertTrue(ApiKeyAuthenticationFilter.isPublicPath("/api/security/status"));
    }

    @Test
    void apiPathsAreNotPublic() {
        assertFalse(ApiKeyAuthenticationFilter.isPublicPath("/api/users/summary"));
        assertFalse(ApiKeyAuthenticationFilter.isPublicPath("/api/sync/starlink"));
    }
}
