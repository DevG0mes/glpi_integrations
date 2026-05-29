package com.devgomes.glpi_integration.session;

import com.devgomes.glpi_integration.client.GlpiApiClient;
import com.devgomes.glpi_integration.client.GlpiApiException;
import com.devgomes.glpi_integration.config.GlpiProperties;
import jakarta.annotation.PreDestroy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class GlpiSessionManager {

    private final GlpiApiClient apiClient;
    private final GlpiProperties properties;

    private volatile String sessionToken;

    public GlpiSessionManager(GlpiApiClient apiClient, GlpiProperties properties) {
        this.apiClient = apiClient;
        this.properties = properties;
    }

    public synchronized String getSessionToken() {
        if (!properties.hasCredentials()) {
            throw new IllegalStateException(
                    "Credenciais GLPI ausentes. Defina GLPI_APP_TOKEN e GLPI_USER_TOKEN (ou application-local.properties).");
        }
        if (sessionToken == null || sessionToken.isBlank()) {
            sessionToken = apiClient.initSession();
        }
        return sessionToken;
    }

    public synchronized void invalidateSession() {
        sessionToken = null;
    }

    public synchronized String refreshSession() {
        invalidateSession();
        return getSessionToken();
    }

    public void runWithSession(GlpiSessionAction action) {
        executeWithSession(token -> {
            action.run(token);
            return null;
        });
    }

    public <T> T executeWithSession(GlpiSessionCallback<T> callback) {
        try {
            return callback.run(getSessionToken());
        } catch (GlpiApiException ex) {
            if (ex.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()) {
                String newToken = refreshSession();
                return callback.run(newToken);
            }
            throw ex;
        }
    }

    @PreDestroy
    public void onShutdown() {
        String token = sessionToken;
        if (token != null) {
            try {
                apiClient.killSession(token);
            } catch (Exception ignored) {
                // encerramento best-effort
            }
        }
    }

    @FunctionalInterface
    public interface GlpiSessionAction {
        void run(String sessionToken);
    }

    @FunctionalInterface
    public interface GlpiSessionCallback<T> {
        T run(String sessionToken);
    }
}
