package com.devgomes.glpi_integration.security;

import com.devgomes.glpi_integration.config.GlpiSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Exige {@code X-API-Key} (ou Bearer) em {@code /api/**} quando {@code glpi.security.api-key} está definida.
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final GlpiSecurityProperties securityProperties;

    public ApiKeyAuthenticationFilter(GlpiSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    static boolean isPublicPath(String path) {
        if (path == null || path.isEmpty()) {
            return true;
        }
        if (path.equals("/") || path.equals("/app") || path.startsWith("/app/")) {
            return true;
        }
        if (path.equals("/actuator/health") || path.startsWith("/actuator/health/")) {
            return true;
        }
        if (path.equals("/api/security/status")) {
            return true;
        }
        return false;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!securityProperties.isApiKeyRequired()) {
            return true;
        }
        String path = request.getRequestURI();
        return isPublicPath(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String provided = extractKey(request);
        if (securityProperties.getApiKey().equals(provided)) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"error\":\"UNAUTHORIZED\",\"message\":\"Chave de API inválida ou ausente (header X-API-Key)\"}");
    }

    private static String extractKey(HttpServletRequest request) {
        String key = request.getHeader("X-API-Key");
        if (key != null && !key.isBlank()) {
            return key.trim();
        }
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return auth.substring(7).trim();
        }
        return "";
    }
}
