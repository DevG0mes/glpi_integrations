package com.devgomes.glpi_integration.config;

/**
 * Normaliza a URL base da API GLPI conforme o estilo configurado.
 * <p>
 * Estilos suportados (validar no servidor com os curls em {@code docs/API_VALIDATION.md}):
 * <ul>
 *   <li>{@code LEGACY_APIREST} — {@code http://host/glpi/apirest.php}</li>
 *   <li>{@code LEGACY_VIA_ROUTER} — {@code http://host/glpi/api.php/v1}</li>
 * </ul>
 */
public final class GlpiApiUrlResolver {

    public enum ApiStyle {
        LEGACY_APIREST,
        LEGACY_VIA_ROUTER
    }

    private GlpiApiUrlResolver() {
    }

    public static String resolveBaseUrl(String configuredUrl, ApiStyle style) {
        if (configuredUrl == null || configuredUrl.isBlank()) {
            throw new IllegalArgumentException("glpi.api.base-url não pode ser vazio");
        }
        String url = configuredUrl.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        return switch (style) {
            case LEGACY_APIREST -> ensureLegacyApirest(url);
            case LEGACY_VIA_ROUTER -> ensureLegacyRouter(url);
        };
    }

    private static String ensureLegacyApirest(String url) {
        if (url.contains("api.php/v")) {
            throw new IllegalArgumentException(
                    "URL parece API v2/v1 router (" + url + "). Use glpi.api.style=LEGACY_VIA_ROUTER ou aponte para apirest.php.");
        }
        if (!url.endsWith("apirest.php")) {
            if (!url.contains("apirest.php")) {
                url = url + "/apirest.php";
            }
        }
        return url;
    }

    private static String ensureLegacyRouter(String url) {
        if (url.endsWith("apirest.php")) {
            return url.replace("apirest.php", "api.php/v1");
        }
        if (!url.contains("api.php/v")) {
            url = url + "/api.php/v1";
        }
        return url;
    }
}
