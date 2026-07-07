package com.devgomes.glpi_integration.service;

import com.devgomes.glpi_integration.client.GlpiApiClient;
import com.devgomes.glpi_integration.session.GlpiSessionManager;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlpiIntegrationServiceTest {

    @Mock
    private GlpiApiClient apiClient;

    @Mock
    private GlpiSessionManager sessionManager;

    private GlpiIntegrationService service;

    @BeforeEach
    void setUp() {
        service = new GlpiIntegrationService(apiClient, sessionManager, new ObjectMapper());
    }

    @Test
    void updateComputer_delegatesWithSession() {
        doAnswer(invocation -> {
            GlpiSessionManager.GlpiSessionAction action = invocation.getArgument(0);
            action.run("token-1");
            return null;
        }).when(sessionManager).runWithSession(any());

        service.updateComputer(10, new com.devgomes.glpi_integration.dto.ComputerUpdateRequest(
                42, null, null, 1, 11, null, null, null, null, null, null, null, null));

        verify(apiClient).updateComputer(eq("token-1"), eq(10), any());
    }
}
