package com.devgomes.glpi_integration.sync;

import com.devgomes.glpi_integration.config.GlpiCustomAssetsProperties;
import com.devgomes.glpi_integration.dto.ComputerUpdateRequest;
import com.devgomes.glpi_integration.service.GlpiIntegrationService;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.LinkedHashMap;
import java.util.List;
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
    private static final DateTimeFormatter BRAZILIAN_DATE_FLEX =
            new DateTimeFormatterBuilder()
                    .appendValue(ChronoField.DAY_OF_MONTH)
                    .appendLiteral('/')
                    .appendValue(ChronoField.MONTH_OF_YEAR)
                    .appendLiteral('/')
                    .appendValue(ChronoField.YEAR, 4)
                    .toFormatter()
                    .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter BRAZILIAN_DATE_SHORT_YEAR_FLEX =
            new DateTimeFormatterBuilder()
                    .appendValue(ChronoField.DAY_OF_MONTH)
                    .appendLiteral('/')
                    .appendValue(ChronoField.MONTH_OF_YEAR)
                    .appendLiteral('/')
                    .appendValueReduced(ChronoField.YEAR, 2, 2, 2000)
                    .toFormatter()
                    .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter BRAZILIAN_DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter BRAZILIAN_DATE_TIME_MINUTES =
            DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter BRAZILIAN_DATE_TIME_FLEX =
            new DateTimeFormatterBuilder()
                    .append(BRAZILIAN_DATE_FLEX)
                    .appendLiteral(' ')
                    .appendValue(ChronoField.HOUR_OF_DAY)
                    .appendLiteral(':')
                    .appendValue(ChronoField.MINUTE_OF_HOUR)
                    .optionalStart()
                    .appendLiteral(':')
                    .appendValue(ChronoField.SECOND_OF_MINUTE)
                    .optionalEnd()
                    .toFormatter()
                    .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter BRAZILIAN_DATE_TIME_SHORT_YEAR_FLEX =
            new DateTimeFormatterBuilder()
                    .append(BRAZILIAN_DATE_SHORT_YEAR_FLEX)
                    .appendLiteral(' ')
                    .appendValue(ChronoField.HOUR_OF_DAY)
                    .appendLiteral(':')
                    .appendValue(ChronoField.MINUTE_OF_HOUR)
                    .optionalStart()
                    .appendLiteral(':')
                    .appendValue(ChronoField.SECOND_OF_MINUTE)
                    .optionalEnd()
                    .toFormatter()
                    .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter ISO_DATE_TIME_MINUTES =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT);
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            ISO_DATE,
            BRAZILIAN_DATE,
            BRAZILIAN_DATE_FLEX,
            BRAZILIAN_DATE_SHORT_YEAR_FLEX
    );
    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = List.of(
            ISO_DATE_TIME,
            ISO_DATE_TIME_MINUTES,
            BRAZILIAN_DATE_TIME,
            BRAZILIAN_DATE_TIME_MINUTES,
            BRAZILIAN_DATE_TIME_FLEX,
            BRAZILIAN_DATE_TIME_SHORT_YEAR_FLEX
    );

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
            case DATETIME -> normalizeDateTime(raw);
        };
    }

    static String normalizeDate(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        return parseDate(value).format(ISO_DATE);
    }

    static String normalizeDateTime(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            return parseDate(value).atStartOfDay().format(ISO_DATE_TIME);
        } catch (IllegalArgumentException ignored) {
            return parseDateTime(value).format(ISO_DATE_TIME);
        }
    }

    private static LocalDate parseDate(String value) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next supported format.
            }
        }
        throw new IllegalArgumentException(
                "Data inválida: '" + value + "'. Use YYYY-MM-DD ou DD/MM/YYYY.");
    }

    private static LocalDateTime parseDateTime(String value) {
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next supported format.
            }
        }
        throw new IllegalArgumentException(
                "Data/hora inválida: '" + value + "'. Use YYYY-MM-DD[ HH:mm[:ss]] ou DD/MM/YYYY[ HH:mm[:ss]].");
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
