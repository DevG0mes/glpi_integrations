package com.devgomes.glpi_integration.sync;

import com.devgomes.glpi_integration.config.GlpiCustomAssetsProperties;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SyncFieldResolverTest {

    @Test
    void toUpdateRequest_resolvesLoginAndStatusLabel() {
        var row = new AssetUpdateRow(
                2, 1558, null, null, "evellyn.cavalcante", null, "Em uso",
                6, "CTVMLZ1", null, null,
                null, null, null, null, null, null, null, null, null,
                "30/06/2026 14:30", "MEGA-001");

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
        assertThat(request.vencimentoGarantia()).isEqualTo("2026-06-30 14:30:00");
        assertThat(request.codMega()).isEqualTo("MEGA-001");
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

    @Test
    void normalizeDateTime_convertsBrazilianDateTime() {
        assertThat(SyncFieldResolver.normalizeDateTime("30/06/2026 14:30"))
                .isEqualTo("2026-06-30 14:30:00");
    }

    @Test
    void normalizeDateTime_defaultsDateOnlyToMidnight() {
        assertThat(SyncFieldResolver.normalizeDateTime("2026-06-30"))
                .isEqualTo("2026-06-30 00:00:00");
    }

    @Test
    void resolveCustomAssetFields_normalizesChipDueDate() {
        var definition = new GlpiCustomAssetsProperties().getDefinition("chip");
        var values = new LinkedHashMap<String, String>();
        values.put("vencimento", "30/06/2026");
        var row = new CustomAssetRow(2, 0, "8900000000000000001", values);

        var fields = SyncFieldResolver.resolveCustomAssetFields("chip", row, definition, SyncLookupIndexes.empty());

        assertThat(fields).containsEntry("custom_vencimento", "2026-06-30");
    }
}
