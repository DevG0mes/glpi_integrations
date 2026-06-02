package com.devgomes.glpi_integration.service;

import com.devgomes.glpi_integration.config.GlpiCustomAssetsProperties;
import com.devgomes.glpi_integration.config.GlpiSyncProperties;
import com.devgomes.glpi_integration.sync.AssetTypeRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomAssetItemWriteServiceTest {

    @Mock
    private GlpiIntegrationService glpiIntegrationService;
    @Mock
    private GlpiSyncProperties syncProperties;

    private CustomAssetItemWriteService service;

    @BeforeEach
    void setUp() {
        GlpiCustomAssetsProperties properties = new GlpiCustomAssetsProperties();
        AssetTypeRegistry registry = new AssetTypeRegistry(properties);
        service = new CustomAssetItemWriteService(registry, glpiIntegrationService, syncProperties);
    }

    @Test
    void create_mapsColaboradorColumns() {
        when(syncProperties.getLookupRange()).thenReturn("0-999");
        when(glpiIntegrationService.initSession()).thenReturn("sess");
        when(glpiIntegrationService.buildFieldValueIndex(anyString(), eq("custom_email"), anyString()))
                .thenReturn(Map.of());
        when(glpiIntegrationService.buildSyncLookupIndexes(anyString()))
                .thenReturn(new com.devgomes.glpi_integration.sync.SyncLookupIndexes(
                        Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of()));
        when(glpiIntegrationService.createItem(anyString(), anyMap())).thenReturn(42);

        Map<String, Object> result = service.create("colaborador", Map.of(
                "nome", "Maria",
                "email", "maria@empresa.com",
                "departamento", "RH",
                "ativo", "Sim"));

        verify(glpiIntegrationService).createItem(
                eq("Glpi\\Asset\\AssetDefinition/Colaborador"),
                org.mockito.ArgumentMatchers.argThat(fields ->
                        "Maria".equals(fields.get("name"))
                                && "maria@empresa.com".equals(fields.get("custom_email"))
                                && "RH".equals(fields.get("custom_departamento"))
                                && "Sim".equals(fields.get("custom_ativo"))));
        org.assertj.core.api.Assertions.assertThat(result.get("itemId")).isEqualTo(42);
    }

    @Test
    void create_rejectsEmptyBody() {
        assertThatThrownBy(() -> service.create("colaborador", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vazio");
    }
}
