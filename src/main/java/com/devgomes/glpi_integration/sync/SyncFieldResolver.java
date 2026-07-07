package com.devgomes.glpi_integration.sync;

import com.devgomes.glpi_integration.config.GlpiCustomAssetsProperties;
import com.devgomes.glpi_integration.dto.ComputerUpdateRequest;
import com.devgomes.glpi_integration.service.GlpiIntegrationService;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Resolve campos textuais da planilha (login de usuário, nome de status) para IDs do GLPI.
 */
public final class SyncFieldResolver {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter ISO_DATE_TIME =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter BRAZILIAN_DATE =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter BRAZILIAN_DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter BRAZILIAN_DATE_TIME_MINUTES =
            DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter ISO_DATE_TIME_MINUTES =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT);

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
                row.comment(),
                normalizeDateTime(row.vencimentoGarantia()),
                normalizeOptionalText(row.codMega())
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
            case DATE -> normalizeDate(raw);
        };
    }

    static String normalizeDate(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return parseDate(value, ISO_DATE).format(ISO_DATE);
        }
        if (value.matches("\\d{2}/\\d{2}/\\d{4}")) {
            return parseDate(value, BRAZILIAN_DATE).format(ISO_DATE);
        }
        throw new IllegalArgumentException(
                "Data inválida: '" + raw + "'. Use YYYY-MM-DD ou DD/MM/YYYY.");
    }

    static String normalizeDateTime(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return parseDate(value, ISO_DATE).atStartOfDay().format(ISO_DATE_TIME);
        }
        if (value.matches("\\d{2}/\\d{2}/\\d{4}")) {
            return parseDate(value, BRAZILIAN_DATE).atStartOfDay().format(ISO_DATE_TIME);
        }
        if (value.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
            return parseDateTime(value, ISO_DATE_TIME).format(ISO_DATE_TIME);
        }
        if (value.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")) {
            return parseDateTime(value, ISO_DATE_TIME_MINUTES).format(ISO_DATE_TIME);
        }
        if (value.matches("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}")) {
            return parseDateTime(value, BRAZILIAN_DATE_TIME).format(ISO_DATE_TIME);
        }
        if (value.matches("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}")) {
            return parseDateTime(value, BRAZILIAN_DATE_TIME_MINUTES).format(ISO_DATE_TIME);
        }
        throw new IllegalArgumentException(
                "Data/hora inválida: '" + raw + "'. Use YYYY-MM-DD[ HH:mm[:ss]] ou DD/MM/YYYY[ HH:mm[:ss]].");
    }

    private static LocalDate parseDate(String value, DateTimeFormatter formatter) {
        try {
            return LocalDate.parse(value, formatter);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "Data inválida: '" + value + "'. Use YYYY-MM-DD ou DD/MM/YYYY.", ex);
        }
    }

    private static LocalDateTime parseDateTime(String value, DateTimeFormatter formatter) {
        try {
            return LocalDateTime.parse(value, formatter);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "Data/hora inválida: '" + value + "'. Use YYYY-MM-DD[ HH:mm[:ss]] ou DD/MM/YYYY[ HH:mm[:ss]].", ex);
        }
    }

    private static String normalizeOptionalText(String raw) {
        if (isNullLiteral(raw)) {
            return null;
        }
        return raw.trim();
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
