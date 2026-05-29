package com.devgomes.glpi_integration.sync;

import com.devgomes.glpi_integration.config.GlpiCustomAssetsProperties;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Chaves naturais por tipo de ativo: planilha ({@code nome}, {@code iccid}, {@code imei}) ↔ GLPI.
 */
public final class CustomAssetNaturalKeySupport {

    private CustomAssetNaturalKeySupport() {
    }

    /** Colunas da planilha aceitas como identificador (além de {@code id_ativo}). */
    public static List<String> spreadsheetAliases(GlpiCustomAssetsProperties.CustomAssetDefinition definition) {
        Set<String> aliases = new LinkedHashSet<>();
        for (Map.Entry<String, GlpiCustomAssetsProperties.FieldMapping> entry : definition.columns().entrySet()) {
            GlpiCustomAssetsProperties.FieldMapping mapping = entry.getValue();
            if (mapping.resolverType() == GlpiCustomAssetsProperties.FieldResolverType.NATURAL_KEY
                    || mapping.glpiField().equals(definition.naturalKeyField())) {
                aliases.add(entry.getKey());
            }
        }
        if ("name".equals(definition.naturalKeyField())) {
            aliases.add("nome");
        }
        return List.copyOf(aliases);
    }

    public static String requiredColumnsHint(GlpiCustomAssetsProperties.CustomAssetDefinition definition) {
        List<String> aliases = spreadsheetAliases(definition);
        if (aliases.isEmpty()) {
            return "id_ativo ou " + definition.naturalKeyField();
        }
        return "id_ativo ou " + String.join(" / ", aliases);
    }

    public static String rowMissingKeyMessage(
            int lineNumber,
            GlpiCustomAssetsProperties.CustomAssetDefinition definition) {
        return "Linha " + lineNumber + ": informe " + requiredColumnsHint(definition);
    }

    public static String resolveFromRow(
            String[] row,
            Map<String, Integer> headerIndex,
            GlpiCustomAssetsProperties.CustomAssetDefinition definition,
            Map<String, String> values) {
        for (String alias : spreadsheetAliases(definition)) {
            String fromColumn = cell(row, headerIndex, alias);
            if (!SyncFieldResolver.isNullLiteral(fromColumn)) {
                return fromColumn.trim();
            }
            String fromValues = values.get(alias);
            if (!SyncFieldResolver.isNullLiteral(fromValues)) {
                return fromValues.trim();
            }
        }
        String glpiField = cell(row, headerIndex, definition.naturalKeyField());
        if (!SyncFieldResolver.isNullLiteral(glpiField)) {
            return glpiField.trim();
        }
        return null;
    }

    public static String valueFromRow(CustomAssetRow row, GlpiCustomAssetsProperties.CustomAssetDefinition definition) {
        if (row.naturalKeyValue() != null && !row.naturalKeyValue().isBlank()) {
            return row.naturalKeyValue().trim();
        }
        for (String alias : spreadsheetAliases(definition)) {
            String value = row.values().get(alias);
            if (!SyncFieldResolver.isNullLiteral(value)) {
                return value.trim();
            }
        }
        return null;
    }

    /** Nome de exibição GLPI ({@code name}) na criação: prefere coluna {@code nome}, senão a chave natural. */
    public static String displayNameForCreate(
            CustomAssetRow row,
            GlpiCustomAssetsProperties.CustomAssetDefinition definition) {
        String nome = row.values().get("nome");
        if (!SyncFieldResolver.isNullLiteral(nome)) {
            return nome.trim();
        }
        return valueFromRow(row, definition);
    }

    private static String cell(String[] row, Map<String, Integer> headerIndex, String column) {
        Integer idx = headerIndex.get(column);
        if (idx == null || idx >= row.length) {
            return null;
        }
        return row[idx];
    }
}
