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

        String key = assetKey.toLowerCase(java.util.Locale.ROOT).trim();
        GlpiCustomAssetsProperties.CustomAssetDefinition def = properties.getDefinition(key);

        if (def == null) {
            throw new IllegalArgumentException("Tipo de ativo desconhecido: " + assetKey
                    + ". Valores mapeados no application.yml: " + keys());
        }

        // Validação defensiva: alerta se o ItemType não possuir o namespace do GLPI 11 para ativos customizados
        String itemType = def.itemType();
        if (itemType != null && !itemType.contains("\\") && !itemType.equalsIgnoreCase("Computer")) {
            // Em vez de cuspir o HTML no front, quebramos aqui se a configuração do YAML estiver errada
            throw new IllegalArgumentException(
                    "O itemType para o ativo '" + key + "' parece incorreto no application.yml. " +
                            "Valor atual: '" + itemType + "'. Esperado algo como 'Glpi\\CustomAsset\\" + itemType + "Asset'."
            );
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