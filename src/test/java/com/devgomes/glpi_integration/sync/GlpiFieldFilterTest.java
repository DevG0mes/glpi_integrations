package com.devgomes.glpi_integration.sync;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlpiFieldFilterTest {

    @Test
    void retainsOnlyKeysPresentOnItem() {
        Map<String, Object> proposed = Map.of(
                "name", "KIT-1",
                "custom_projeto", "PCS",
                "custom_senha_conta_starlink", "secret",
                "custom_unknown", "x");
        Map<String, Object> existing = Map.of(
                "id", 1,
                "name", "old",
                "custom_projeto", "old-p");

        GlpiFieldFilter.FilterResult result = GlpiFieldFilter.retainFieldsKnownToItem(proposed, existing);

        assertThat(result.fields()).containsEntry("name", "KIT-1");
        assertThat(result.fields()).containsEntry("custom_projeto", "PCS");
        assertThat(result.droppedFieldNames()).containsExactlyInAnyOrder(
                "custom_unknown");
    }

    @Test
    void copiesEntitiesIdFromExistingItem() {
        Map<String, Object> proposed = Map.of("name", "KIT-1");
        Map<String, Object> existing = Map.of("id", 1, "name", "old", "entities_id", 5);

        GlpiFieldFilter.FilterResult result = GlpiFieldFilter.retainFieldsKnownToItem(proposed, existing);

        assertThat(result.fields()).containsEntry("entities_id", 5);
    }

    @Test
    void keepsCustomFieldsEvenWhenGetItemDoesNotExposeThem() {
        Map<String, Object> proposed = Map.of(
                "custom_vencimento", "2026-06-30",
                "name", "Chip 1");
        Map<String, Object> existing = Map.of(
                "id", 1,
                "name", "old");

        GlpiFieldFilter.FilterResult result = GlpiFieldFilter.retainFieldsKnownToItem(proposed, existing);

        assertThat(result.fields()).containsEntry("custom_vencimento", "2026-06-30");
        assertThat(result.droppedFieldNames()).isEmpty();
    }
}
