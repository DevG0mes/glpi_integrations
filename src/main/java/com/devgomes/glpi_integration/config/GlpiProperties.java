package com.devgomes.glpi_integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "glpi.api")
public class GlpiProperties {

    /**
     * URL base da API legada, ex.: {@code http://servidor/glpi/apirest.php}
     */
    private String baseUrl = "http://localhost/glpi/apirest.php";

    /**
     * LEGACY_APIREST (apirest.php) ou LEGACY_VIA_ROUTER (api.php/v1).
     */
    private GlpiApiUrlResolver.ApiStyle style = GlpiApiUrlResolver.ApiStyle.LEGACY_APIREST;

    private String appToken = "";

    private String userToken = "";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public GlpiApiUrlResolver.ApiStyle getStyle() {
        return style;
    }

    public void setStyle(GlpiApiUrlResolver.ApiStyle style) {
        this.style = style;
    }

    public String getAppToken() {
        return appToken;
    }

    public void setAppToken(String appToken) {
        this.appToken = appToken;
    }

    public String getUserToken() {
        return userToken;
    }

    public void setUserToken(String userToken) {
        this.userToken = userToken;
    }

    public boolean hasCredentials() {
        return appToken != null && !appToken.isBlank()
                && userToken != null && !userToken.isBlank();
    }
}
