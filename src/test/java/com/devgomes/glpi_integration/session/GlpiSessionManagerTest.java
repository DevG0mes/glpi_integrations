package com.devgomes.glpi_integration.session;

import com.devgomes.glpi_integration.client.GlpiApiClient;
import com.devgomes.glpi_integration.client.GlpiApiException;
import com.devgomes.glpi_integration.config.GlpiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlpiSessionManagerTest {

    @Mock
    private GlpiApiClient apiClient;

    private GlpiProperties properties;
    private GlpiSessionManager sessionManager;

    @BeforeEach
    void setUp() {
        properties = new GlpiProperties();
        properties.setAppToken("app");
        properties.setUserToken("user");
        sessionManager = new GlpiSessionManager(apiClient, properties);
    }

    @Test
    void getSessionToken_reusesCachedToken() {
        when(apiClient.initSession()).thenReturn("sess-1");

        assertThat(sessionManager.getSessionToken()).isEqualTo("sess-1");
        assertThat(sessionManager.getSessionToken()).isEqualTo("sess-1");

        verify(apiClient, times(1)).initSession();
    }

    @Test
    void executeWithSession_retriesOnUnauthorized() {
        when(apiClient.initSession()).thenReturn("sess-1", "sess-2");

        sessionManager.getSessionToken();

        String result = sessionManager.executeWithSession(token -> {
            if ("sess-1".equals(token)) {
                throw new GlpiApiException(HttpStatus.UNAUTHORIZED, "expired");
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        verify(apiClient, times(2)).initSession();
    }
}
