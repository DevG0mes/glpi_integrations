package com.devgomes.glpi_integration.service;

import com.devgomes.glpi_integration.client.GlpiItemTypePath;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Carrega definições de ativo do GLPI 11 e associa ao assetKey do middleware (starlink, chip, celular).
 */
@Component
public class GlpiAssetDefinitionCatalog {

    private static final List<String> DEFINITION_LIST_ITEMTYPES = List.of(
            "Glpi\\Asset\\AssetDefinition",
            "AssetDefinition",
            "Glpi\\Asset\\AssetModel",
            "Assets\\AssetDefinition"
    );

    private static final Map<String, List<String>> MATCH_ALIASES = Map.of(
            "starlink", List.of("starlink"),
            "chip", List.of("chip", "sim", "carte"),
            "celular", List.of("celular", "phone", "telefone", "mobile")
    );

    private final GlpiIntegrationService glpiIntegrationService;

    public GlpiAssetDefinitionCatalog(GlpiIntegrationService glpiIntegrationService) {
        this.glpiIntegrationService = glpiIntegrationService;
    }

    public List<Map<String, Object>> fetchDefinitions() {
        return fetchDefinitionsWithDiagnostics().definitions();
    }

    public FetchDefinitionsResult fetchDefinitionsWithDiagnostics() {
        List<Map<String, Object>> attempts = new ArrayList<>();
        List<Map<String, Object>> bestRows = List.of();

        for (String itemType : DEFINITION_LIST_ITEMTYPES) {
            Map<String, Object> attempt = tryListDefinitions(itemType);
            attempts.add(attempt);
            if (Boolean.TRUE.equals(attempt.get("reachable"))) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> rows = (List<Map<String, Object>>) attempt.get("rows");
                if (rows != null && !rows.isEmpty()) {
                    return new FetchDefinitionsResult(rows, attempts, null);
                }
                if (rows != null && bestRows.isEmpty()) {
                    bestRows = rows;
                }
            }
        }

        Map<String, Object> computerCheck = tryListDefinitions("Computer");
        attempts.add(0, computerCheck);

        String warning = null;
        if (!Boolean.TRUE.equals(computerCheck.get("reachable"))) {
            warning = "API GLPI inacessível ou credenciais inválidas (Computer também falhou).";
        } else if (bestRows.isEmpty()) {
            warning = "Computer OK, mas nenhuma definição de ativo listada. "
                    + "Verifique Configuração → Ativos no GLPI e permissões do perfil da API "
                    + "(Configuração → Geral → API / perfil do usuário do token).";
        }

        return new FetchDefinitionsResult(bestRows, attempts, warning);
    }

    private Map<String, Object> tryListDefinitions(String itemType) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("itemType", itemType);
        row.put("itemTypeEncoded", GlpiItemTypePath.encode(itemType));
        try {
            List<Map<String, Object>> items = glpiIntegrationService.listGlpiItemRows(itemType, "0-199");
            row.put("reachable", true);
            row.put("rowCount", items.size());
            row.put("rows", items);
        } catch (Exception ex) {
            row.put("reachable", false);
            row.put("error", shorten(ex.getMessage()));
        }
        return row;
    }

    public Optional<Map<String, Object>> matchDefinition(String assetKey, List<Map<String, Object>> definitions) {
        if (definitions == null || definitions.isEmpty()) {
            return Optional.empty();
        }
        String key = assetKey.toLowerCase(Locale.ROOT);
        List<String> needles = MATCH_ALIASES.getOrDefault(key, List.of(key));

        Map<String, Object> best = null;
        int bestScore = 0;

        for (Map<String, Object> defRow : definitions) {
            String systemName = stringField(defRow, "system_name");
            String label = firstNonBlank(
                    stringField(defRow, "name"),
                    stringField(defRow, "label"),
                    stringField(defRow, "completename"));

            int score = scoreMatch(needles, key, systemName, label);
            if (score > bestScore) {
                bestScore = score;
                best = defRow;
            }
        }

        return bestScore > 0 ? Optional.of(best) : Optional.empty();
    }

    public Map<String, Object> toSummary(Map<String, Object> definitionRow) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", definitionRow.get("id"));
        summary.put("system_name", stringField(definitionRow, "system_name"));
        summary.put("label", firstNonBlank(
                stringField(definitionRow, "name"),
                stringField(definitionRow, "label")));
        summary.put("is_active", definitionRow.get("is_active"));
        return summary;
    }

    private static int scoreMatch(List<String> needles, String assetKey, String systemName, String label) {
        int score = 0;
        String sn = systemName == null ? "" : systemName.toLowerCase(Locale.ROOT);
        String lb = label == null ? "" : label.toLowerCase(Locale.ROOT);

        if (sn.equals(assetKey) || lb.equals(assetKey)) {
            return 100;
        }
        for (String needle : needles) {
            if (sn.equals(needle) || lb.equals(needle)) {
                score = Math.max(score, 90);
            } else if (sn.contains(needle) || lb.contains(needle)) {
                score = Math.max(score, 70);
            } else if (needle.contains(sn) && !sn.isBlank()) {
                score = Math.max(score, 50);
            }
        }
        return score;
    }

    private static String shorten(String message) {
        if (message == null) {
            return "";
        }
        return message.length() > 300 ? message.substring(0, 300) + "..." : message;
    }

    private static String stringField(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? "" : value.toString().trim();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    public record FetchDefinitionsResult(
            List<Map<String, Object>> definitions,
            List<Map<String, Object>> listAttempts,
            String warning
    ) {
    }
}
