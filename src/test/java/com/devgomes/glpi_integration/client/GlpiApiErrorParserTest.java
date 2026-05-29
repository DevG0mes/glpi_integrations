package com.devgomes.glpi_integration.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlpiApiErrorParserTest {

    @Test
    void addsHintWhenGlpiUpdateMessageIsNull() {
        String parsed = GlpiApiErrorParser.humanMessage("[\"ERROR_GLPI_UPDATE\",null]");
        assertThat(parsed).contains("ERROR_GLPI_UPDATE");
        assertThat(parsed).contains("Atualizar todos");
    }
}
