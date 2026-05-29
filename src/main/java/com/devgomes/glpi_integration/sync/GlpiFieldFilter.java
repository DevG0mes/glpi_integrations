package com.devgomes.glpi_integration.sync;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reduz o payload de PUT aos campos que o GLPI expõe no GET do item.
 * Evita {@code ERROR_GLPI_UPDATE} por chaves {@code custom_*} com nome de sistema incorreto.
 */
public final class GlpiFieldFilter {

    private static final Set<String> CONTEXT_FIELDS = Set.of(
            "entities_id", "is_recursive", "is_deleted", "is_template", "is_dynamic"
    );

    private GlpiFieldFilter() {
    }

    public static FilterResult retainFieldsKnownToItem(
            Map<String, Object> proposed,
            Map<String, Object> existingItem) {
        if (existingItem == null || existingItem.isEmpty()) {
            return new FilterResult(proposed, List.of());
        }
        Set<String> itemKeys = existingItem.keySet();
        Map<String, Object> retained = new LinkedHashMap<>();
        List<String> dropped = new java.util.ArrayList<>();

        for (Map.Entry<String, Object> entry : proposed.entrySet()) {
            String key = entry.getKey();
            if (itemKeys.contains(key)) {
                retained.put(key, entry.getValue());
            } else {
                dropped.add(key);
            }
        }

        for (String key : itemKeys) {
            if (key.contains("assetdefinitions") || CONTEXT_FIELDS.contains(key)) {
                copyContextIfAbsent(retained, existingItem, key);
            }
        }
        for (String key : CONTEXT_FIELDS) {
            copyContextIfAbsent(retained, existingItem, key);
        }

        return new FilterResult(retained, dropped);
    }

    private static void copyContextIfAbsent(
            Map<String, Object> retained,
            Map<String, Object> existingItem,
            String key) {
        if (!retained.containsKey(key) && existingItem.get(key) != null) {
            retained.put(key, existingItem.get(key));
        }
    }

    public record FilterResult(Map<String, Object> fields, List<String> droppedFieldNames) {
    }
}
