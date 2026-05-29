package com.devgomes.glpi_integration.sync;

import com.devgomes.glpi_integration.config.GlpiCustomAssetsProperties;
import com.devgomes.glpi_integration.dto.ComputerUpdateRequest;
import com.devgomes.glpi_integration.service.GlpiIntegrationService;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Resolve campos textuais da planilha (login de usuário, nome de status) para IDs do GLPI.
 */
public final class SyncFieldResolver {

    private SyncFieldResolver() {
    }

    public static ComputerUpdateRequest toUpdateRequest(AssetUpdateRow row, SyncLookupIndexes indexes) {
        Integer usersId = resolveUserId(row.usersId(), row.responsibleLogin(), indexes.usersByLogin());
        Integer statesId = resolveStateId(row.statesId(), row.statusLabel(), indexes.statesByLabel());
        Integer groupsId = resolveId(row.groupsId(), row.groupLabel(), indexes.groupsByLabel());
        Integer locationsId = resolveId(row.locationsId(), row.locationLabel(), indexes.locationsByLabel());
        Integer typesId = resolveId(row.computertypesId(), row.computerTypeLabel(), indexes.computerTypesByLabel());
        Integer manufacturersId = resolveId(row.manufacturersId(), row.manufacturerLabel(), indexes.manufacturersByLabel());

        return new ComputerUpdateRequest(
                usersId,
                groupsId,
                locationsId,
                statesId,
                row.computermodelsId(),
                typesId,
                manufacturersId,
                row.displayName(),
                row.serial(),
                row.otherserial(),
                row.comment()
        );
    }

    public static Map<String, Object> resolveCustomAssetFields(
            String assetKey,
            CustomAssetRow row,
            GlpiCustomAssetsProperties.CustomAssetDefinition definition,
            SyncLookupIndexes indexes) {
        Map<String, Object> input = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : row.values().entrySet()) {
            GlpiCustomAssetsProperties.FieldMapping mapping = definition.columns().get(entry.getKey());
            if (mapping == null) {
                continue;
            }
            if (isNullLiteral(entry.getValue()) || entry.getValue().trim().isEmpty()) {
                continue;
            }
            Object value = resolveCustomValue(mapping, entry.getValue(), indexes);
            if (value != null) {
                input.put(mapping.glpiField(), value);
            }
        }
        return input;
    }

    private static Object resolveCustomValue(
            GlpiCustomAssetsProperties.FieldMapping mapping,
            String raw,
            SyncLookupIndexes indexes) {
        if (SyncFieldResolver.isNullLiteral(raw)) {
            return null;
        }
        return switch (mapping.resolverType()) {
            case DIRECT, SENSITIVE, NATURAL_KEY -> raw.trim();
            case USER_LOGIN -> {
                if (isNumeric(raw)) {
                    yield Integer.parseInt(raw.trim());
                }
                yield indexes.usersByLogin().get(normalizeLabelKey(raw));
            }
            case STATE_LABEL -> {
                if (isNumeric(raw)) {
                    yield Integer.parseInt(raw.trim());
                }
                yield indexes.statesByLabel().get(normalizeLabelKey(raw));
            }
            case LOCATION_LABEL -> {
                if (isNumeric(raw)) {
                    yield Integer.parseInt(raw.trim());
                }
                yield indexes.locationsByLabel().get(normalizeLabelKey(raw));
            }
        };
    }

    private static Integer resolveUserId(Integer id, String label, Map<String, Integer> index) {
        if (id != null) {
            return id;
        }
        if (label == null) {
            return null;
        }
        return index.get(normalizeLabelKey(label));
    }

    private static Integer resolveStateId(Integer id, String label, Map<String, Integer> index) {
        if (id != null) {
            return id;
        }
        if (label == null) {
            return null;
        }
        return index.get(normalizeLabelKey(label));
    }

    private static Integer resolveId(Integer id, String label, Map<String, Integer> index) {
        if (id != null) {
            return id;
        }
        if (label == null) {
            return null;
        }
        return index.get(normalizeLabelKey(label));
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
