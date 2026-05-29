package com.devgomes.glpi_integration.sync;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SyncFieldResolverTest {

    @Test
    void toUpdateRequest_resolvesLoginAndStatusLabel() {
        var row = new AssetUpdateRow(
                2, 1558, null, null, "evellyn.cavalcante", null, "Em uso",
                6, "CTVMLZ1", null, null);

        var request = SyncFieldResolver.toUpdateRequest(
                row,
                Map.of("evellyn.cavalcante", 99),
                Map.of("em uso", 3));

        assertThat(request.usersId()).isEqualTo(99);
        assertThat(request.statesId()).isEqualTo(3);
        assertThat(request.computermodelsId()).isEqualTo(6);
        assertThat(request.serial()).isEqualTo("CTVMLZ1");
    }

    @Test
    void normalizeLabelKey_ignoresAccentsAndCase() {
        assertThat(SyncFieldResolver.normalizeLabelKey("Disponível"))
                .isEqualTo("disponivel");
    }

    @Test
    void isNullLiteral_treatsNullStringAsEmpty() {
        assertThat(SyncFieldResolver.isNullLiteral("null")).isTrue();
        assertThat(SyncFieldResolver.isNullLiteral("NULL")).isTrue();
    }
}
