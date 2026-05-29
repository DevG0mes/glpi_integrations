package com.devgomes.glpi_integration.sync;

import java.util.Map;

public record SyncLookupIndexes(
        Map<String, Integer> usersByLogin,
        Map<String, Integer> statesByLabel,
        Map<String, Integer> locationsByLabel,
        Map<String, Integer> groupsByLabel,
        Map<String, Integer> computerTypesByLabel,
        Map<String, Integer> manufacturersByLabel
) {
    public static SyncLookupIndexes empty() {
        return new SyncLookupIndexes(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
    }
}
