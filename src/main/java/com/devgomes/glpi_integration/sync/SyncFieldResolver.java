package com.devgomes.glpi_integration.sync;

import com.devgomes.glpi_integration.dto.ComputerUpdateRequest;
import com.devgomes.glpi_integration.service.GlpiIntegrationService;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

/**
 * Resolve campos textuais da planilha (login de usuário, nome de status) para IDs do GLPI.
 */
public final class SyncFieldResolver {

    private SyncFieldResolver() {
    }

    public static ComputerUpdateRequest toUpdateRequest(
            AssetUpdateRow row,
            Map<String, Integer> usersByLogin,
            Map<String, Integer> statesByLabel) {
        Integer usersId = row.usersId();
        if (usersId == null && row.responsibleLogin() != null) {
            usersId = usersByLogin.get(normalizeLabelKey(row.responsibleLogin()));
        }

        Integer statesId = row.statesId();
        if (statesId == null && row.statusLabel() != null) {
            statesId = statesByLabel.get(normalizeLabelKey(row.statusLabel()));
        }

        return new ComputerUpdateRequest(
                usersId,
                null,
                null,
                statesId,
                row.computermodelsId(),
                null,
                null,
                null,
                row.serial(),
                row.otherserial(),
                row.comment()
        );
    }

    public static String normalizeLabelKey(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        String withoutAccents = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return withoutAccents;
    }

    public static boolean isNullLiteral(String value) {
        if (value == null) {
            return true;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty()
                || "null".equalsIgnoreCase(trimmed)
                || "-".equals(trimmed);
    }

    public static boolean isNumeric(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            Integer.parseInt(value.trim());
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public static int parsePositiveInt(String value) {
        int parsed = Integer.parseInt(value.trim());
        if (parsed <= 0) {
            throw new NumberFormatException("ID deve ser positivo: " + value);
        }
        return parsed;
    }

    public static String normalizeNameKey(String name) {
        return GlpiIntegrationService.normalizeNameKey(name);
    }
}
