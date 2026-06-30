package com.devgomes.glpi_integration.sync;

import com.devgomes.glpi_integration.config.GlpiCustomAssetsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CustomAssetSpreadsheetReaderTest {

    private CustomAssetSpreadsheetReader reader;

    @BeforeEach
    void setUp() {
        var properties = new GlpiCustomAssetsProperties();
        reader = new CustomAssetSpreadsheetReader(new AssetTypeRegistry(properties));
    }

    @Test
    void read_parsesChipCsv() throws Exception {
        Path file = Path.of(new ClassPathResource("sample-chip.csv").getURI());

        var rows = reader.read(file, "chip");

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().naturalKeyValue()).isEqualTo("8900000000000000001");
        assertThat(rows.getFirst().values()).containsEntry("numero", "11999990001");
        assertThat(rows.getFirst().values()).containsEntry("vencimento", "30/06/2026");
    }

    @Test
    void read_parsesCelularCsv() throws Exception {
        Path file = Path.of(new ClassPathResource("sample-celular.csv").getURI());

        var rows = reader.read(file, "celular");

        assertThat(rows.getFirst().values()).containsEntry("imei", "350000000000001");
        assertThat(rows.getFirst().values()).containsEntry("modelo", "Galaxy A14");
    }
}
