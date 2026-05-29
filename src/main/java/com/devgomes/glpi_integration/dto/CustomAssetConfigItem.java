package com.devgomes.glpi_integration.dto;

import java.util.List;
import java.util.Map;

public record CustomAssetConfigItem(
        String key,
        String itemType,
        String itemTypeEncoded,
        boolean configured,
        String naturalKeyField,
        List<String> spreadsheetColumns,
        Map<String, String> columnToGlpiField
) {
}
