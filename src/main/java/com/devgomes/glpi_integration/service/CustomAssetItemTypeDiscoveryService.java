package com.devgomes.glpi_integration.service;

import com.devgomes.glpi_integration.client.GlpiItemTypePath;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Testa candidatos de itemtype GLPI 11 até encontrar um que responda na API legada.
 */
@Service
public class CustomAssetItemTypeDiscoveryService {

    private final GlpiIntegrationService glpiIntegrationService;
    private final GlpiAssetDefinitionCatalog definitionCatalog;

    public CustomAssetItemTypeDiscoveryService(
            GlpiIntegrationService glpiIntegrationService,
            GlpiAssetDefinitionCatalog definitionCatalog) {
        this.glpiIntegrationService = glpiIntegrationService;
        this.definitionCatalog = definitionCatalog;
    }

    public Map<String, Object> discoverAll() {
        glpiIntegrationService.initSession();
        var fetchResult = definitionCatalog.fetchDefinitionsWithDiagnostics();
        List<Map<String, Object>> definitions = fetchResult.definitions();
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : List.of("starlink", "chip", "celular")) {
            result.put(key, discoverOne(key, false, definitions));
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("hint", "Copie resolvedItemType para application-local.properties (GLPI_ITEMTYPE_*)");
        response.put("propertiesExample", buildPropertiesExample(result));
        response.put("glpiAssetDefinitions", summarizeDefinitions(definitions));
        response.put("definitionListAttempts", fetchResult.listAttempts());
        response.put("assets", result);
        if (fetchResult.warning() != null) {
            response.put("definitionsWarning", fetchResult.warning());
        }
        return response;
    }

    public Map<String, Object> discoverOne(String assetKey) {
        glpiIntegrationService.initSession();
        return discoverOne(assetKey, true, definitionCatalog.fetchDefinitionsWithDiagnostics().definitions());
    }

    private Map<String, Object> discoverOne(
            String assetKey,
            boolean initSession,
            List<Map<String, Object>> definitions) {
        if (initSession) {
            glpiIntegrationService.initSession();
        }
        String label = labelForKey(assetKey);
        Optional<Map<String, Object>> matchedDefinition = definitionCatalog.matchDefinition(assetKey, definitions);

        List<String> systemNamesFromGlpi = new ArrayList<>();
        matchedDefinition.ifPresent(def -> {
            Object sn = def.get("system_name");
            if (sn != null && !sn.toString().isBlank()) {
                systemNamesFromGlpi.add(sn.toString().trim());
            }
        });

        List<Map<String, Object>> attempts = new ArrayList<>();
        String resolved = null;

        for (String candidate : GlpiCustomAssetItemTypeCandidates.forAsset(assetKey, label, systemNamesFromGlpi)) {
            Map<String, Object> attempt = tryList(candidate);
            attempts.add(attempt);
            if (Boolean.TRUE.equals(attempt.get("reachable"))) {
                resolved = candidate;
                break;
            }
        }

        if (resolved == null && !definitions.isEmpty()) {
            resolved = bruteForceByDefinitions(assetKey, definitions, attempts);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("assetKey", assetKey);
        out.put("resolvedItemType", resolved);
        out.put("resolvedItemTypeEncoded", resolved != null ? GlpiItemTypePath.encode(resolved) : null);
        out.put("configured", resolved != null);
        matchedDefinition.ifPresent(def -> out.put("matchedDefinition", definitionCatalog.toSummary(def)));
        out.put("attempts", attempts);
        if (resolved == null) {
            out.put("nextStep",
                    "Em Configuração → Ativos, copie o «nome do sistema» da definição. "
                            + "Teste no Postman: GET .../apirest.php/Glpi%5CAsset%5CAssetDefinition/SEU_NOME/?range=0-0 "
                            + "ou use GET /api/custom-assets/definitions e informe ?systemName= na descoberta.");
            out.put("documentationPattern", "Glpi\\Asset\\AssetDefinition/{system_name}");
        }
        return out;
    }

    /**
     * Para cada definição no GLPI, testa {@code Glpi\Asset\AssetDefinition/{system_name}}.
     */
    private String bruteForceByDefinitions(
            String assetKey,
            List<Map<String, Object>> definitions,
            List<Map<String, Object>> attempts) {
        for (Map<String, Object> def : definitions) {
            Object sn = def.get("system_name");
            if (sn == null || sn.toString().isBlank()) {
                continue;
            }
            if (!definitionCatalog.matchDefinition(assetKey, List.of(def)).isPresent()) {
                continue;
            }
            String candidate = "Glpi\\Asset\\AssetDefinition/" + sn.toString().trim();
            Map<String, Object> attempt = tryList(candidate);
            attempts.add(attempt);
            if (Boolean.TRUE.equals(attempt.get("reachable"))) {
                return candidate;
            }
        }
        return null;
    }

    private List<Map<String, Object>> summarizeDefinitions(List<Map<String, Object>> definitions) {
        return definitions.stream().map(definitionCatalog::toSummary).toList();
    }

    public Map<String, Object> discoverWithSystemName(String assetKey, String systemName) {
        glpiIntegrationService.initSession();
        List<String> names = systemName == null || systemName.isBlank()
                ? List.of()
                : List.of(systemName.trim());
        String label = labelForKey(assetKey);

        List<Map<String, Object>> attempts = new ArrayList<>();
        String resolved = null;
        for (String candidate : GlpiCustomAssetItemTypeCandidates.forAsset(assetKey, label, names)) {
            Map<String, Object> attempt = tryList(candidate);
            attempts.add(attempt);
            if (Boolean.TRUE.equals(attempt.get("reachable"))) {
                resolved = candidate;
                break;
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("assetKey", assetKey);
        out.put("systemNameProvided", systemName);
        out.put("resolvedItemType", resolved);
        out.put("resolvedItemTypeEncoded", resolved != null ? GlpiItemTypePath.encode(resolved) : null);
        out.put("configured", resolved != null);
        out.put("attempts", attempts);
        return out;
    }

    private Map<String, Object> tryList(String itemType) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("itemType", itemType);
        row.put("itemTypeEncoded", GlpiItemTypePath.encode(itemType));
        try {
            int count = glpiIntegrationService.listCustomAssetIdAndNames(itemType, "0-0").size();
            row.put("reachable", true);
            row.put("sampleCount", count);
        } catch (Exception ex) {
            row.put("reachable", false);
            row.put("error", shorten(ex.getMessage()));
        }
        return row;
    }

    private static String labelForKey(String assetKey) {
        return switch (assetKey.toLowerCase(Locale.ROOT)) {
            case "starlink" -> "Starlink";
            case "chip" -> "Chip";
            case "celular" -> "Celular";
            default -> assetKey;
        };
    }

    private static String shorten(String message) {
        if (message == null) {
            return "";
        }
        return message.length() > 200 ? message.substring(0, 200) + "..." : message;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> buildPropertiesExample(Map<String, Object> assets) {
        Map<String, String> lines = new LinkedHashMap<>();
        Map<String, Object> assetMap = (Map<String, Object>) assets;
        putPropertyLine(lines, "GLPI_ITEMTYPE_STARLINK", assetMap.get("starlink"));
        putPropertyLine(lines, "GLPI_ITEMTYPE_CHIP", assetMap.get("chip"));
        putPropertyLine(lines, "GLPI_ITEMTYPE_CELULAR", assetMap.get("celular"));
        return lines;
    }

    @SuppressWarnings("unchecked")
    private static void putPropertyLine(Map<String, String> lines, String envKey, Object discoveryResult) {
        if (!(discoveryResult instanceof Map<?, ?> map)) {
            lines.put(envKey, "# não resolvido automaticamente");
            return;
        }
        Object resolved = map.get("resolvedItemType");
        if (resolved == null) {
            lines.put(envKey, "# não resolvido — veja attempts e glpiAssetDefinitions");
            return;
        }
        String escaped = resolved.toString().replace("\\", "\\\\");
        lines.put(envKey, escaped);
    }
}
