package com.devgomes.glpi_integration.dto;

import java.util.List;
import java.util.Map;

public record ComputerListResponse(
        String range,
        String contentRange,
        int returned,
        List<Map<String, Object>> items
) {
}
