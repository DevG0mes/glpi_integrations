package com.devgomes.glpi_integration.sync;

import com.devgomes.glpi_integration.config.GlpiCustomAssetsProperties;
import com.devgomes.glpi_integration.config.GlpiSyncProperties;
import com.devgomes.glpi_integration.service.GlpiIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class GenericItemSyncBatchServiceTest {

    @Mock
    private GlpiIntegrationService glpiIntegrationService;

    private GenericItemSyncBatchService batchService;

    @BeforeEach
    void setUp() {
        GlpiSyncProperties syncProperties = new GlpiSyncProperties();
        syncProperties.setDelayMs(0);
        var properties = new GlpiCustomAssetsProperties();
        batchService = new GenericItemSyncBatchService(
                null,
                new AssetTypeRegistry(properties),
                glpiIntegrationService,
                syncProperties);
    }

    @Test
    void processRows_dryRun_masksPasswords() {
        var row = new CustomAssetRow(2, 10, null, Map.of(
                "projeto", "Alpha",
                "senha_conta", "secret1",
                "senha_roteador", "secret2"
        ));

        when(glpiIntegrationService.initSession()).thenReturn("sess");
        when(glpiIntegrationService.buildSyncLookupIndexes(any())).thenReturn(SyncLookupIndexes.empty());
        when(glpiIntegrationService.buildFieldValueIndex(any(), any(), any())).thenReturn(Map.of());

        SyncReport report = batchService.processRows("starlink", "test.csv", List.of(row), true);

        assertThat(report.successCount()).isEqualTo(1);
        assertThat(report.lines().getFirst().message()).contains("***");
        verify(glpiIntegrationService, never()).updateItem(any(), eq(10), any());
    }
}
