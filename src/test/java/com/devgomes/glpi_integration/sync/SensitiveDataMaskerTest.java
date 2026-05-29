package com.devgomes.glpi_integration.sync;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataMaskerTest {

    @Test
    void maskFields_hidesPasswordLikeKeys() {
        var masked = SensitiveDataMasker.maskFields(
                Map.of("projeto", "A", "senha_roteador", "x", "email", "a@b.com"),
                Set.of());

        assertThat(masked.get("senha_roteador")).isEqualTo("***");
        assertThat(masked.get("projeto")).isEqualTo("A");
    }
}
