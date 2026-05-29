package com.devgomes.glpi_integration.sync;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class SensitiveDataMasker {

    private static final String MASK = "***";

    private SensitiveDataMasker() {
    }

    public static Map<String, Object> maskFields(Map<String, Object> input, Set<String> sensitiveKeys) {
        Map<String, Object> masked = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            if (entry.getValue() != null && isSensitiveKey(entry.getKey(), sensitiveKeys)) {
                masked.put(entry.getKey(), MASK);
            } else {
                masked.put(entry.getKey(), entry.getValue());
            }
        }
        return masked;
    }

    private static boolean isSensitiveKey(String key, Set<String> sensitiveKeys) {
        if (sensitiveKeys.contains(key)) {
            return true;
        }
        String lower = key.toLowerCase();
        return lower.contains("senha") || lower.contains("password") || lower.contains("secret");
    }
}
