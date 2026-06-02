package com.devgomes.glpi_integration.sync;

import com.devgomes.glpi_integration.config.GlpiCustomAssetsProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class AssetTypeRegistry {

    private final GlpiCustomAssetsProperties properties;

    public AssetTypeRegistry(GlpiCustomAssetsProperties properties) {
        this.properties = properties;
    }

    public Set<String> keys() {
        return Set.copyOf(properties.getTypes().keySet());
    }

    public GlpiCustomAssetsProperties.CustomAssetDefinition get(String assetKey) {
        if (assetKey == null || assetKey.isBlank()) {
            throw new IllegalArgumentException("assetKey obrigatório");
        }
        String key = assetKey.toLowerCase(java.util.Locale.ROOT);
        GlpiCustomAssetsProperties.CustomAssetDefinition def = properties.getDefinition(key);
        if (def == null) {
            throw new IllegalArgumentException("Tipo de ativo desconhecido: " + assetKey
                    + ". Valores: " + keys());
        }
        return def;
    }

    public boolean isSensitiveField(String assetKey, String glpiField) {
        GlpiCustomAssetsProperties.CustomAssetDefinition def = get(assetKey);
        return def.sensitiveFieldNames().stream().anyMatch(s -> s.equalsIgnoreCase(glpiField))
                || def.columns().values().stream()
                .anyMatch(m -> m.glpiField().equals(glpiField)
                        && m.resolverType() == GlpiCustomAssetsProperties.FieldResolverType.SENSITIVE);
    }
}
