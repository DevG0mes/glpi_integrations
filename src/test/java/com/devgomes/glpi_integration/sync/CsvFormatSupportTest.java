package com.devgomes.glpi_integration.sync;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CsvFormatSupportTest {

    @Test
    void detectSeparator_prefersSemicolonForExcelExport() {
        assertThat(CsvFormatSupport.detectSeparator("a;b;c")).isEqualTo(';');
        assertThat(CsvFormatSupport.detectSeparator("a,b,c")).isEqualTo(',');
    }

    @Test
    void stripBom_removesUtf8Bom() {
        assertThat(CsvFormatSupport.stripBom("\ufeffid_ativo")).isEqualTo("id_ativo");
    }
}
