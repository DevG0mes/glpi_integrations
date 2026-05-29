package com.devgomes.glpi_integration.sync;

import java.util.List;

public record SyncReport(
        String source,
        int total,
        int successCount,
        int failureCount,
        List<SyncLineResult> lines
) {
}
