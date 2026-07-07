package com.devgomes.glpi_integration.sync;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        assertThat(rows.getFirst().vencimentoGarantia()).isEqualTo("30/06/2026 14:30");
        assertThat(rows.getFirst().codMega()).isEqualTo("MEGA-001");
        assertThat(rows.getFirst().usesNameLookup()).isFalse();
        assertThat(rows.get(1).responsibleLogin()).isNull();
        assertThat(rows.get(1).statusLabel()).isEqualTo("Estoque");
        assertThat(rows.get(1).vencimentoGarantia()).isNull();
        assertThat(rows.get(1).codMega()).isNull();
    }

    @Test
    void read_parsesSemicolonSeparatedCsv() throws Exception {
        Path file = Path.of(new ClassPathResource("sample-computers-semicolon.csv").getURI());

        var rows = reader.read(file);

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().glpiId()).isEqualTo(1558);
        assertThat(rows.getFirst().computermodelsId()).isEqualTo(1);
        assertThat(rows.getFirst().vencimentoGarantia()).isEqualTo("2026-06-30");
        assertThat(rows.getFirst().codMega()).isEqualTo("MEGA-001");
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

    @Test
    void read_allowsMissingIdAtivoWhenAtivoColumnIsPresent() throws Exception {
        Path file = Files.createTempFile("sample-computers-sem-id", ".csv");
        Files.writeString(file, """
                Service TAG,ATIVO,id_model,RESPONSAVEL,Status
                9TCMLZ1,PSI-016,1,evellyn.cavalcante,Em uso
                """);

        var rows = reader.read(file);

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().glpiId()).isZero();
        assertThat(rows.getFirst().assetName()).isEqualTo("PSI-016");
        assertThat(rows.getFirst().displayName()).isEqualTo("PSI-016");
    }

    @Test
    void read_rejectsSpreadsheetWithoutIdAndWithoutNameLikeColumn() throws Exception {
        Path file = Files.createTempFile("sample-computers-invalid", ".csv");
        Files.writeString(file, """
                Service TAG,id_model,RESPONSAVEL,Status
                9TCMLZ1,1,evellyn.cavalcante,Em uso
                """);

        assertThatThrownBy(() -> reader.read(file))
                .hasMessageContaining("Colunas insuficientes para identificar o Computer");
    }
}
