package com.devgomes.glpi_integration.sync;

import com.devgomes.glpi_integration.config.GlpiCustomAssetsProperties;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CustomAssetNaturalKeySupportTest {

    private final GlpiCustomAssetsProperties properties = new GlpiCustomAssetsProperties();

    @Test
    void starlinkAliasesIncludeNome() {
        var def = properties.getDefinition("starlink");
        assertThat(CustomAssetNaturalKeySupport.spreadsheetAliases(def)).contains("nome");
    }

    @Test
    void chipAliasesIncludeIccid() {
        var def = properties.getDefinition("chip");
        assertThat(CustomAssetNaturalKeySupport.spreadsheetAliases(def)).contains("iccid");
    }

    @Test
    void celularDisplayNamePrefersNome() {
        var def = properties.getDefinition("celular");
        CustomAssetRow row = new CustomAssetRow(
                2,
                0,
                "351234567890999",
                Map.of("nome", "Celular Novo", "imei", "351234567890999"));
        assertThat(CustomAssetNaturalKeySupport.displayNameForCreate(row, def)).isEqualTo("Celular Novo");
    }

    @Test
    void chipDisplayNameFallsBackToIccid() {
        var def = properties.getDefinition("chip");
        CustomAssetRow row = new CustomAssetRow(2, 0, "8955012345678901999", Map.of("iccid", "8955012345678901999"));
        assertThat(CustomAssetNaturalKeySupport.displayNameForCreate(row, def)).isEqualTo("8955012345678901999");
    }
}
