package com.devgomes.glpi_integration.sync;

import com.devgomes.glpi_integration.client.GlpiApiException;
import com.devgomes.glpi_integration.config.GlpiSyncProperties;
import com.devgomes.glpi_integration.dto.ComputerUpdateRequest;
import com.devgomes.glpi_integration.service.GlpiIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ComputerSyncBatchServiceTest {

    @Mock
    private AssetSpreadsheetReader spreadsheetReader;

    @Mock
    private GlpiIntegrationService glpiIntegrationService;

    private ComputerSyncBatchService batchService;

    @BeforeEach
    void setUp() {
        GlpiSyncProperties syncProperties = new GlpiSyncProperties();
        syncProperties.setDelayMs(0);
        batchService = new ComputerSyncBatchService(spreadsheetReader, glpiIntegrationService, syncProperties);
    }

    @Test
    void processRows_continuesOnFailureAndBuildsReport() {
        var rows = List.of(
                new AssetUpdateRow(2, 10, null, null, null, null, null, 11, "SN-1", null, null),
                new AssetUpdateRow(3, 11, null, null, null, null, null, 12, "SN-2", null, null)
        );

        when(glpiIntegrationService.buildComputerNameIndex(any())).thenReturn(Map.of());
        when(glpiIntegrationService.buildUserLoginIndex(any())).thenReturn(Map.of());
        when(glpiIntegrationService.buildStateLabelIndex(any())).thenReturn(Map.of());

        doThrow(new GlpiApiException(HttpStatus.BAD_REQUEST, "erro linha 2"))
                .when(glpiIntegrationService).updateComputer(eq(10), any(ComputerUpdateRequest.class));

        SyncReport report = batchService.processRows("test.csv", rows);

        assertThat(report.total()).isEqualTo(2);
        assertThat(report.successCount()).isEqualTo(1);
        assertThat(report.failureCount()).isEqualTo(1);
        assertThat(report.lines().get(0).success()).isFalse();
        assertThat(report.lines().get(1).success()).isTrue();
        verify(glpiIntegrationService).initSession();
    }
}
