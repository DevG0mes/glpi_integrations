package com.devgomes.glpi_integration.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GlpiCustomAssetItemTypeCandidatesTest {

    @Test
    void forAsset_putsAssetDefinitionSlashPatternFirst() {
        List<String> candidates = GlpiCustomAssetItemTypeCandidates.forAsset("starlink", "Starlink");

        assertThat(candidates.getFirst()).isEqualTo("Glpi\\Asset\\AssetDefinition/Starlink");
    }

    @Test
    void forAsset_usesGlpiSystemNameWhenProvided() {
        List<String> candidates = GlpiCustomAssetItemTypeCandidates.forAsset(
                "chip", "Chip", List.of("CarteSim"));

        assertThat(candidates.getFirst()).isEqualTo("Glpi\\Asset\\AssetDefinition/CarteSim");
        assertThat(candidates).contains("Glpi\\CustomAsset\\CarteSimAsset");
    }
}
