package com.devgomes.glpi_integration.sync;

import com.devgomes.glpi_integration.config.GlpiCustomAssetsProperties;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomAssetTargetResolverTest {

    private final GlpiCustomAssetsProperties.CustomAssetDefinition starlink =
            new GlpiCustomAssetsProperties().getDefinition("starlink");

    @Test
    void idAtivoForcesUpdate() {
        CustomAssetRow row = new CustomAssetRow(2, 1, "KIT-X", Map.of("nome", "KIT-X"));
        ResolvedAssetTarget target = CustomAssetTargetResolver.resolve(row, starlink, Map.of());
        assertThat(target.create()).isFalse();
        assertThat(target.itemId()).isEqualTo(1);
        assertThat(target.matchedBy()).contains("id_ativo");
    }

    @Test
    void unknownNameCreates() {
        CustomAssetRow row = new CustomAssetRow(2, 0, null, Map.of("nome", "KIT-NOVO-999"));
        ResolvedAssetTarget target = CustomAssetTargetResolver.resolve(row, starlink, Map.of());
        assertThat(target.create()).isTrue();
    }

    @Test
    void chipUnknownIccidCreates() {
        var chip = new GlpiCustomAssetsProperties().getDefinition("chip");
        CustomAssetRow row = new CustomAssetRow(2, 0, "8955099999999999999", Map.of("iccid", "8955099999999999999"));
        ResolvedAssetTarget target = CustomAssetTargetResolver.resolve(row, chip, Map.of());
        assertThat(target.create()).isTrue();
    }

    @Test
    void existingNameUpdates() {
        Map<String, Integer> index = Map.of("kit402606346w5m", 1);
        CustomAssetRow row = new CustomAssetRow(2, 0, null, Map.of("nome", "KIT402606346W5M"));
        ResolvedAssetTarget target = CustomAssetTargetResolver.resolve(row, starlink, index);
        assertThat(target.create()).isFalse();
        assertThat(target.itemId()).isEqualTo(1);
    }

    @Test
    void requiresIdOrName() {
        CustomAssetRow row = new CustomAssetRow(2, 0, null, new LinkedHashMap<>());
        assertThatThrownBy(() -> CustomAssetTargetResolver.resolve(row, starlink, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
