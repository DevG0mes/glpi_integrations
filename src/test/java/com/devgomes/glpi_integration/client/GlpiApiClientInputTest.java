package com.devgomes.glpi_integration.client;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlpiApiClientInputTest {

    @Test
    void enrichInput_addsId() {
        Map<String, Object> input = GlpiApiClient.enrichInput(1, Map.of("name", "test"));
        assertThat(input).containsEntry("id", 1).containsEntry("name", "test");
    }
}
