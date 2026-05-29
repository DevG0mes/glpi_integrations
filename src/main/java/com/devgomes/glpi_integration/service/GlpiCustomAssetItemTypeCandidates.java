package com.devgomes.glpi_integration.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Candidatos de itemtype para ativos GLPI 11 (Asset Definitions).
 * <p>
 * Formato confirmado no fórum GLPI 11.0.5 para listar/criar itens:
 * {@code Glpi\Asset\AssetDefinition/{system_name}} → URL {@code Glpi%5CAsset%5CAssetDefinition/Starlink}.
 * <p>
 * Classe PHP documentada: system name {@code Example} → {@code Glpi\CustomAsset\ExampleAsset}.
 */
public final class GlpiCustomAssetItemTypeCandidates {

    private GlpiCustomAssetItemTypeCandidates() {
    }

    public static List<String> forAsset(String assetKey, String displayLabel) {
        return forAsset(assetKey, displayLabel, List.of());
    }

    public static List<String> forAsset(String assetKey, String displayLabel, List<String> glpiSystemNames) {
        Set<String> candidates = new LinkedHashSet<>();
        for (String systemName : distinctSystemNames(assetKey, displayLabel, glpiSystemNames)) {
            addGlpi11Patterns(candidates, systemName);
        }
        addLegacyAndHeuristicPatterns(candidates, displayLabel, assetKey);
        return List.copyOf(candidates);
    }

    private static List<String> distinctSystemNames(String assetKey, String displayLabel, List<String> fromGlpi) {
        Set<String> names = new LinkedHashSet<>();
        if (fromGlpi != null) {
            for (String name : fromGlpi) {
                addIfPresent(names, name);
            }
        }
        for (String variant : systemNameVariants(assetKey, displayLabel)) {
            addIfPresent(names, variant);
        }
        return List.copyOf(names);
    }

    private static void addGlpi11Patterns(Set<String> candidates, String systemName) {
        if (systemName == null || systemName.isBlank()) {
            return;
        }
        // GLPI 11 — padrão do fórum (prioridade máxima)
        candidates.add("Glpi\\Asset\\AssetDefinition/" + systemName);

        String withAssetSuffix = systemName.endsWith("Asset") ? systemName : systemName + "Asset";
        candidates.add("Glpi\\CustomAsset\\" + withAssetSuffix);
        candidates.add("GlpiCustomAsset" + withAssetSuffix);

        candidates.add("Glpi\\Asset\\AssetDefinition\\" + systemName);
        candidates.add("Glpi\\Asset\\CustomAsset\\" + systemName);
        candidates.add("Glpi\\CustomAsset\\" + systemName);
        candidates.add("GlpiCustomAsset" + systemName);
    }

    private static void addLegacyAndHeuristicPatterns(Set<String> candidates, String label, String assetKey) {
        String capitalized = capitalize(label);
        String upper = label.toUpperCase(Locale.ROOT);
        candidates.add(capitalized);
        candidates.add(label);
        if (assetKey != null && !assetKey.equalsIgnoreCase(label)) {
            candidates.add(capitalize(assetKey));
            candidates.add(assetKey);
        }
        candidates.add(upper);
        candidates.add("AssetDefinition" + capitalized);
        candidates.add("PluginGenericobject" + capitalized);
    }

    static List<String> systemNameVariants(String assetKey, String displayLabel) {
        List<String> names = new ArrayList<>();
        addIfPresent(names, displayLabel);
        addIfPresent(names, capitalize(displayLabel));
        if (assetKey != null) {
            addIfPresent(names, assetKey);
            addIfPresent(names, capitalize(assetKey));
            addIfPresent(names, assetKey.toUpperCase(Locale.ROOT));
        }
        return names;
    }

    private static void addIfPresent(Set<String> names, String value) {
        if (value != null && !value.isBlank()) {
            names.add(value.trim());
        }
    }

    private static void addIfPresent(List<String> names, String value) {
        if (value != null && !value.isBlank()) {
            String trimmed = value.trim();
            if (!names.contains(trimmed)) {
                names.add(trimmed);
            }
        }
    }

    private static String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1).toLowerCase(Locale.ROOT);
    }
}
