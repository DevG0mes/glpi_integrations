package com.devgomes.glpi_integration.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GlpiApiUrlResolverTest {

    @Test
    void resolveBaseUrl_legacyApirest_appendsWhenMissing() {
        String url = GlpiApiUrlResolver.resolveBaseUrl(
                "http://host/glpi",
                GlpiApiUrlResolver.ApiStyle.LEGACY_APIREST);

        assertThat(url).isEqualTo("http://host/glpi/apirest.php");
    }

    @Test
    void resolveBaseUrl_legacyRouter_convertsApirest() {
        String url = GlpiApiUrlResolver.resolveBaseUrl(
                "http://host/glpi/apirest.php",
                GlpiApiUrlResolver.ApiStyle.LEGACY_VIA_ROUTER);

        assertThat(url).isEqualTo("http://host/glpi/api.php/v1");
    }

    @Test
    void resolveBaseUrl_rejectsV2UrlForLegacyApirest() {
        assertThatThrownBy(() -> GlpiApiUrlResolver.resolveBaseUrl(
                "http://host/api.php/v2.2",
                GlpiApiUrlResolver.ApiStyle.LEGACY_APIREST))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
