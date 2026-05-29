package com.devgomes.glpi_integration.service;

import com.devgomes.glpi_integration.client.GlpiApiException;
import com.devgomes.glpi_integration.client.GlpiItemTypePath;
import com.devgomes.glpi_integration.config.GlpiCustomAssetsProperties;
import com.devgomes.glpi_integration.sync.AssetTypeRegistry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Testa PUT campo a campo para diagnosticar {@code ERROR_GLPI_UPDATE}.
 */
@Service
public class CustomAssetUpdateProbeService {

    private final GlpiIntegrationService glpiIntegrationService;
    private final AssetTypeRegistry assetTypeRegistry;

    public CustomAssetUpdateProbeService(
            GlpiIntegrationService glpiIntegrationService,
            AssetTypeRegistry assetTypeRegistry) {
        this.glpiIntegrationService = glpiIntegrationService;
        this.assetTypeRegistry = assetTypeRegistry;
    }

    public Map<String, Object> probe(String assetKey, int itemId, Map<String, Object> fieldsToTest) {
        GlpiCustomAssetsProperties.CustomAssetDefinition definition = assetTypeRegistry.get(assetKey);
        glpiIntegrationService.initSession();

        Map<String, Object> existing;
        try {
            existing = glpiIntegrationService.getItem(definition.itemType(), itemId, false);
        } catch (Exception ex) {
            return Map.of(
                    "assetKey", assetKey,
                    "itemId", itemId,
                    "reachable", false,
                    "error", "GET falhou: " + ex.getMessage(),
                    "hint", "Confirme id_ativo e GET /api/custom-assets/" + assetKey + "/items/" + itemId
            );
        }

        String mutationType = GlpiItemTypePath.mutationItemType(definition.itemType());
        List<Map<String, Object>> attempts = new ArrayList<>();

        Map<String, Object> baseline = Map.of("comment", probeComment(existing));
        attempts.add(tryField(definition.itemType(), itemId, "comment (baseline)", baseline));

        if (fieldsToTest != null) {
            for (Map.Entry<String, Object> entry : fieldsToTest.entrySet()) {
                attempts.add(tryField(
                        definition.itemType(),
                        itemId,
                        entry.getKey(),
                        Map.of(entry.getKey(), entry.getValue())));
            }
        }

        List<String> customKeys = existing.keySet().stream()
                .map(Object::toString)
                .filter(k -> k.startsWith("custom_"))
                .sorted()
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("assetKey", assetKey);
        result.put("itemId", itemId);
        result.put("listItemType", definition.itemType());
        result.put("mutationItemType", mutationType);
        result.put("customFieldKeysOnItem", customKeys);
        result.put("entitiesId", existing.get("entities_id"));
        result.put("attempts", attempts);
        result.put("hint",
                "Se todos os PUT falharem com ERROR_GLPI_UPDATE: em GLPI vá em Configuração → Ativos → "
                        + displayName(assetKey)
                        + " → aba Perfis e marque «Atualizar todos» para o perfil vinculado ao usuário do token API.");
        return result;
    }

    private static String probeComment(Map<String, Object> existing) {
        Object current = existing.get("comment");
        String base = current != null ? current.toString() : "";
        if (base.startsWith("probe-")) {
            return base.endsWith("-b") ? base.substring(0, base.length() - 1) + "a" : base + "-b";
        }
        return "probe-sync";
    }

    private Map<String, Object> tryField(
            String itemType,
            int itemId,
            String label,
            Map<String, Object> fields) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("field", label);
        row.put("payload", fields);
        try {
            glpiIntegrationService.updateItem(itemType, itemId, fields, true);
            row.put("success", true);
        } catch (GlpiApiException ex) {
            row.put("success", false);
            row.put("error", ex.getMessage());
        } catch (Exception ex) {
            row.put("success", false);
            row.put("error", ex.getMessage());
        }
        return row;
    }

    private static String displayName(String assetKey) {
        return switch (assetKey.toLowerCase()) {
            case "starlink" -> "Starlink";
            case "chip" -> "Chip";
            case "celular" -> "Celular";
            default -> assetKey;
        };
    }
}
