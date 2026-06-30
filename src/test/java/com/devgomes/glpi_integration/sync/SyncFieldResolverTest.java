package com.devgomes.glpi_integration.sync;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SyncFieldResolverTest {

    @Test
    void toUpdateRequest_resolvesLoginAndStatusLabel() {
        var row = new AssetUpdateRow(
                2, 1558, null, null, "evellyn.cavalcante", null, "Em uso",
                6, "CTVMLZ1", null, null,
                null, null, null, null, null, null, null, null, null);

        var indexes = new SyncLookupIndexes(
                Map.of("evellyn.cavalcante", 99),
                Map.of("em uso", 3),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of());

        var request = SyncFieldResolver.toUpdateRequest(row, indexes);

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

    @Test
    void normalizeDate_keepsIsoDate() {
        assertThat(SyncFieldResolver.normalizeDate("2026-06-30")).isEqualTo("2026-06-30");
    }

    @Test
    void normalizeDate_convertsBrazilianDate() {
        assertThat(SyncFieldResolver.normalizeDate("30/06/2026")).isEqualTo("2026-06-30");
    }

    @Test
    void normalizeDate_rejectsInvalidDate() {
        assertThatThrownBy(() -> SyncFieldResolver.normalizeDate("30-06-26"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Use YYYY-MM-DD ou DD/MM/YYYY");
    }
}
