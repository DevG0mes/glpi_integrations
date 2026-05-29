package com.devgomes.glpi_integration.client;

import java.util.ArrayList;
import java.util.List;

/**
 * Monta paths da API legada GLPI 11 para itemtypes com namespace.
 * <p>
 * Use {@link #toPathSegments(String)} no RestClient ({@code pathSegment}) para não duplicar
 * codificação de {@code %5C}. Formato confirmado no fórum:
 * {@code Glpi\Asset\AssetDefinition/{system_name}}.
 */
public final class GlpiItemTypePath {

    private GlpiItemTypePath() {
    }

    /**
     * Segmentos de path (barras invertidas literais; o RestClient codifica cada segmento uma vez).
     */
    public static List<String> toPathSegments(String itemType) {
        if (itemType == null || itemType.isBlank()) {
            return List.of();
        }
        String normalized = itemType
                .replace("%5C", "\\")
                .replace("%5c", "\\")
                .trim();

        List<String> segments = new ArrayList<>();
        if (normalized.contains("/")) {
            for (String part : normalized.split("/", -1)) {
                if (part != null && !part.isBlank()) {
                    segments.add(part.trim());
                }
            }
        } else {
            segments.add(normalized);
        }
        return List.copyOf(segments);
    }

    /**
     * Representação legível da URL (para diagnóstico).
     */
    public static String encode(String itemType) {
        if (itemType == null || itemType.isBlank()) {
            return itemType;
        }
        List<String> segments = toPathSegments(itemType);
        if (segments.isEmpty()) {
            return itemType;
        }
        StringBuilder path = new StringBuilder();
        for (int i = 0; i < segments.size(); i++) {
            if (i > 0) {
                path.append('/');
            }
            path.append(segments.get(i).replace("\\", "%5C"));
        }
        return path.toString();
    }

    public static boolean isPlaceholder(String itemType) {
        return itemType != null && itemType.startsWith("CONFIGURE_NO_GLPI");
    }

    /**
     * Itemtype para PUT/POST de itens (classe CommonDBTM concreta).
     * Listagem usa {@code Glpi\Asset\AssetDefinition/{system_name}}; alteração usa {@code Glpi\CustomAsset\{Name}Asset}.
     */
    public static String mutationItemType(String listItemType) {
        String systemName = extractSystemName(listItemType);
        if (systemName == null || systemName.isBlank()) {
            return listItemType;
        }
        String suffix = systemName.endsWith("Asset") ? systemName : systemName + "Asset";
        return "Glpi\\CustomAsset\\" + suffix;
    }

    static String extractSystemName(String itemType) {
        if (itemType == null || itemType.isBlank()) {
            return null;
        }
        String normalized = itemType
                .replace("%5C", "\\")
                .replace("%5c", "\\")
                .trim();
        int slash = normalized.indexOf('/');
        if (slash >= 0 && slash < normalized.length() - 1) {
            return normalized.substring(slash + 1).trim();
        }
        int marker = normalized.indexOf("AssetDefinition\\");
        if (marker >= 0) {
            return normalized.substring(marker + "AssetDefinition\\".length()).trim();
        }
        return null;
    }
}
