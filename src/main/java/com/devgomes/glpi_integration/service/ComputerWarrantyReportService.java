package com.devgomes.glpi_integration.service;

import com.devgomes.glpi_integration.config.GlpiCustomAssetsProperties;
import com.devgomes.glpi_integration.sync.AssetTypeRegistry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ComputerWarrantyReportService {

    private final GlpiIntegrationService glpiIntegrationService;
    private final AssetTypeRegistry assetTypeRegistry;

    public ComputerWarrantyReportService(
            GlpiIntegrationService glpiIntegrationService,
            AssetTypeRegistry assetTypeRegistry) {
        this.glpiIntegrationService = glpiIntegrationService;
        this.assetTypeRegistry = assetTypeRegistry;
    }

    public Map<String, Object> buildReport(String range) {
        List<Map<String, Object>> computers = glpiIntegrationService.listComputerRows(range);
        GlpiCustomAssetsProperties.CustomAssetDefinition garantiaDef = assetTypeRegistry.get("garantia");

        List<Map<String, Object>> garantias;
        String warning = null;
        try {
            garantias = glpiIntegrationService.listGlpiItemRows(garantiaDef.itemType(), "0-9999");
        } catch (Exception ex) {
            garantias = List.of();
            warning = "Nao foi possivel carregar garantias: " + ex.getMessage();
        }

        Map<String, List<Map<String, Object>>> byPatrimonio = indexByTextField(garantias, "name");
        Map<String, List<Map<String, Object>>> bySerial = indexByTextField(garantias, "custom_numero_de_serie");

        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> computer : computers) {
            String patrimonio = stringValue(computer.get("name"));
            String serial = stringValue(computer.get("serial"));

            List<Map<String, Object>> matches = new ArrayList<>();
            Set<String> matchedBy = new LinkedHashSet<>();

            addMatches(matches, matchedBy, byPatrimonio.get(normalizeKey(patrimonio)), "patrimonio");
            addMatches(matches, matchedBy, bySerial.get(normalizeKey(serial)), "serial");

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("computerId", computer.get("id"));
            row.put("patrimonio", patrimonio);
            row.put("serial", serial);
            row.put("garantiaCount", matches.size());
            row.put("matchedBy", List.copyOf(matchedBy));
            row.put("garantiaStatus", matches.stream()
                    .map(item -> stringValue(item.get("states_id")))
                    .filter(value -> !value.isBlank())
                    .distinct()
                    .toList());
            row.put("garantiaVencimentos", matches.stream()
                    .map(item -> stringValue(item.get("custom_vencimento_garantia")))
                    .filter(value -> !value.isBlank())
                    .distinct()
                    .toList());
            row.put("computer", computer);
            row.put("garantias", matches);
            items.add(row);
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("range", range);
        report.put("returned", items.size());
        report.put("garantiaItemType", garantiaDef.itemType());
        if (warning != null) {
            report.put("warning", warning);
        }
        report.put("items", items);
        return report;
    }

    private static Map<String, List<Map<String, Object>>> indexByTextField(
            List<Map<String, Object>> rows,
            String field) {
        Map<String, List<Map<String, Object>>> index = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String key = normalizeKey(stringValue(row.get(field)));
            if (key.isBlank()) {
                continue;
            }
            index.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
        }
        return index;
    }

    private static void addMatches(
            List<Map<String, Object>> target,
            Set<String> matchedBy,
            List<Map<String, Object>> source,
            String rule) {
        if (source == null || source.isEmpty()) {
            return;
        }
        matchedBy.add(rule);
        Set<String> existingIds = new LinkedHashSet<>();
        for (Map<String, Object> item : target) {
            existingIds.add(stringValue(item.get("id")));
        }
        for (Map<String, Object> item : source) {
            String id = stringValue(item.get("id"));
            if (existingIds.add(id)) {
                target.add(item);
            }
        }
    }

    private static String normalizeKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }
}
