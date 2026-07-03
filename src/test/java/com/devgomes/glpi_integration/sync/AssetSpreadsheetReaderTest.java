package com.devgomes.glpi_integration.sync;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AssetSpreadsheetReaderTest {

    private final AssetSpreadsheetReader reader = new AssetSpreadsheetReader();

    @Test
    void read_parsesBusinessCsvColumns() throws Exception {
        Path file = Path.of(new ClassPathResource("sample-computers.csv").getURI());

        var rows = reader.read(file);

        assertThat(rows).hasSize(4);
        assertThat(rows.getFirst().glpiId()).isEqualTo(1558);
        assertThat(rows.getFirst().serial()).isEqualTo("9TCMLZ1");
        assertThat(rows.getFirst().computermodelsId()).isEqualTo(1);
        assertThat(rows.getFirst().responsibleLogin()).isEqualTo("evellyn.cavalcante");
        assertThat(rows.getFirst().statusLabel()).isEqualTo("Em uso");
        assertThat(rows.getFirst().usesNameLookup()).isFalse();
        assertThat(rows.get(1).responsibleLogin()).isNull();
        assertThat(rows.get(1).statusLabel()).isEqualTo("Estoque");
    }

    @Test
    void read_parsesSemicolonSeparatedCsv() throws Exception {
        Path file = Path.of(new ClassPathResource("sample-computers-semicolon.csv").getURI());

        var rows = reader.read(file);

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().glpiId()).isEqualTo(1558);
        assertThat(rows.getFirst().computermodelsId()).isEqualTo(1);
    }

    @Test
    void normalizeHeader_mapsServiceTagColumn() {
        assertThat(AssetSpreadsheetReader.normalizeHeader("Service TAG")).isEqualTo("service_tag");
    }

    @Test
    void normalizeHeader_stripsParentheses() {
        assertThat(AssetSpreadsheetReader.normalizeHeader("Service tAG (Serial Number)"))
                .isEqualTo("service_tag_serial_number");
        assertThat(AssetSpreadsheetReader.normalizeHeader("id_ativo (name)"))
                .isEqualTo("id_ativo_name");
        assertThat(AssetSpreadsheetReader.normalizeHeader("id_model(modelo)"))
                .contains("id_model");
    }

    @Test
    void read_mapsAtivoColumnToDisplayName() throws Exception {
        Path file = Files.createTempFile("sample-computers-ativo", ".csv");
        Files.writeString(file, """
                Service TAG,ATIVO,id_ativo,id_model,RESPONSAVEL,Status
                9TCMLZ1,PSI-016,1558,1,evellyn.cavalcante,Em uso
                """);

        var rows = reader.read(file);

        assertThat(rows.getFirst().displayName()).isEqualTo("PSI-016");
    }
}
