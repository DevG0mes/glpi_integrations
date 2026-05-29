package com.devgomes.glpi_integration.sync;

public record SyncLineResult(
        int lineNumber,
        int glpiId,
        boolean success,
        String message
) {
}
