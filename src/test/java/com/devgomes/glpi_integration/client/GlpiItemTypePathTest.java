package com.devgomes.glpi_integration.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlpiItemTypePathTest {

    @Test
    void toPathSegments_splitsSlashFormat() {
        assertThat(GlpiItemTypePath.toPathSegments("Glpi\\Asset\\AssetDefinition/Starlink"))
                .containsExactly("Glpi\\Asset\\AssetDefinition", "Starlink");
    }

    @Test
    void toPathSegments_singleSegmentForComputer() {
        assertThat(GlpiItemTypePath.toPathSegments("Computer"))
                .containsExactly("Computer");
    }

    @Test
    void encode_replacesBackslashesForGlpi11() {
        assertThat(GlpiItemTypePath.encode("Glpi\\Asset\\AssetDefinition\\Starlink"))
                .isEqualTo("Glpi%5CAsset%5CAssetDefinition%5CStarlink");
    }

    @Test
    void encode_assetDefinitionSlashPath() {
        assertThat(GlpiItemTypePath.encode("Glpi\\Asset\\AssetDefinition/Starlink"))
                .isEqualTo("Glpi%5CAsset%5CAssetDefinition/Starlink");
    }

    @Test
    void encode_leavesComputerUnchanged() {
        assertThat(GlpiItemTypePath.encode("Computer")).isEqualTo("Computer");
    }

    @Test
    void mutationItemType_fromAssetDefinitionSlash() {
        assertThat(GlpiItemTypePath.mutationItemType("Glpi\\Asset\\AssetDefinition/Starlink"))
                .isEqualTo("Glpi\\CustomAsset\\StarlinkAsset");
    }

    @Test
    void extractSystemName_fromSlashPath() {
        assertThat(GlpiItemTypePath.extractSystemName("Glpi\\Asset\\AssetDefinition/Chip"))
                .isEqualTo("Chip");
    }
}
