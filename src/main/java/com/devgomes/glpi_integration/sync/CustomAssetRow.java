package com.devgomes.glpi_integration.sync;

import java.util.Map;

public record CustomAssetRow(
        int lineNumber,
        int glpiId,
        String naturalKeyValue,
        Map<String, String> values
) {
}
