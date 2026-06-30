package com.devgomes.glpi_integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Definições dos ativos customizados GLPI 11 (Starlink, Chip, Celular, Colaborador).
 * Ajuste {@code item-type} após criar as definições no GLPI — ver docs/CUSTOM_ASSETS.md.
 */
@ConfigurationProperties(prefix = "glpi.custom-assets")
public class GlpiCustomAssetsProperties {

    private Map<String, CustomAssetDefinition> types = defaultTypes();

    public Map<String, CustomAssetDefinition> getTypes() {
        return types;
    }

    public void setTypes(Map<String, CustomAssetDefinition> types) {
        this.types = types != null ? types : defaultTypes();
    }

    public CustomAssetDefinition getDefinition(String key) {
        CustomAssetDefinition defaults = defaultTypes().get(key);
        if (defaults == null) {
            return null;
        }
        CustomAssetDefinition override = types.get(key);
        if (override == null) {
            return defaults;
        }
        return new CustomAssetDefinition(
                coalesce(override.itemType(), defaults.itemType()),
                coalesce(override.naturalKeyField(), defaults.naturalKeyField()),
                defaults.sensitiveFieldNames(),
                defaults.columns()
        );
    }

    private static String coalesce(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static Map<String, CustomAssetDefinition> defaultTypes() {
        Map<String, CustomAssetDefinition> map = new LinkedHashMap<>();
        map.put("starlink", starlinkDefaults());
        map.put("chip", chipDefaults());
        map.put("celular", celularDefaults());
        map.put("colaborador", colaboradorDefaults());
        return map;
    }

    private static CustomAssetDefinition starlinkDefaults() {
        return new CustomAssetDefinition(
                "Glpi\\Asset\\AssetDefinition/Starlink",
                "name",
                List.of("custom_senha_conta_starlink", "custom_senha_roteador", "senha_conta", "senha_roteador"),
                Map.ofEntries(
                        Map.entry("nome", new FieldMapping("name", FieldResolverType.DIRECT)),
                        Map.entry("projeto", new FieldMapping("custom_projeto", FieldResolverType.DIRECT)),
                        Map.entry("responsavel", new FieldMapping("users_id", FieldResolverType.USER_LOGIN)),
                        Map.entry("usuario", new FieldMapping("users_id", FieldResolverType.USER_LOGIN)),
                        Map.entry("email", new FieldMapping("custom_email", FieldResolverType.DIRECT)),
                        Map.entry("senha_conta", new FieldMapping("custom_senha_conta_starlink", FieldResolverType.SENSITIVE)),
                        Map.entry("senha_conta_starlink", new FieldMapping("custom_senha_conta_starlink", FieldResolverType.SENSITIVE)),
                        Map.entry("senha_roteador", new FieldMapping("custom_senha_roteador", FieldResolverType.SENSITIVE)),
                        // Localização como texto (campo custom no GLPI 11); não usar locations_id (dropdown antigo)
                        Map.entry("localidade", new FieldMapping("custom_localizacao", FieldResolverType.DIRECT)),
                        Map.entry("localizacao", new FieldMapping("custom_localizacao", FieldResolverType.DIRECT))
                )
        );
    }

    private static CustomAssetDefinition chipDefaults() {
        return new CustomAssetDefinition(
                "Glpi\\Asset\\AssetDefinition/Chip",
                "custom_iccid",
                List.of(),
                Map.ofEntries(
                        Map.entry("iccid", new FieldMapping("custom_iccid", FieldResolverType.NATURAL_KEY)),
                        Map.entry("nome", new FieldMapping("name", FieldResolverType.DIRECT)),
                        Map.entry("numero", new FieldMapping("custom_numero", FieldResolverType.DIRECT)),
                        Map.entry("responsavel", new FieldMapping("users_id", FieldResolverType.USER_LOGIN)),
                        Map.entry("usuario", new FieldMapping("users_id", FieldResolverType.USER_LOGIN)),
                        Map.entry("status", new FieldMapping("states_id", FieldResolverType.STATE_LABEL)),
                        Map.entry("vencimento", new FieldMapping("custom_vencimento", FieldResolverType.DATE))
                )
        );
    }

    private static CustomAssetDefinition celularDefaults() {
        return new CustomAssetDefinition(
                "Glpi\\Asset\\AssetDefinition/Celular",
                "custom_imei",
                List.of(),
                Map.ofEntries(
                        Map.entry("nome", new FieldMapping("name", FieldResolverType.DIRECT)),
                        Map.entry("imei", new FieldMapping("custom_imei", FieldResolverType.NATURAL_KEY)),
                        Map.entry("modelo", new FieldMapping("custom_modelo", FieldResolverType.DIRECT)),
                        Map.entry("responsavel", new FieldMapping("users_id", FieldResolverType.USER_LOGIN)),
                        Map.entry("usuario", new FieldMapping("users_id", FieldResolverType.USER_LOGIN))
                )
        );
    }

    private static CustomAssetDefinition colaboradorDefaults() {
        return new CustomAssetDefinition(
                "Glpi\\Asset\\AssetDefinition/Colaborador",
                "custom_email",
                List.of(),
                Map.ofEntries(
                        Map.entry("nome", new FieldMapping("name", FieldResolverType.DIRECT)),
                        Map.entry("name", new FieldMapping("name", FieldResolverType.DIRECT)),
                        Map.entry("email", new FieldMapping("custom_email", FieldResolverType.NATURAL_KEY)),
                        Map.entry("departamento", new FieldMapping("custom_departamento", FieldResolverType.DIRECT)),
                        Map.entry("ativo", new FieldMapping("custom_ativo", FieldResolverType.DIRECT))
                )
        );
    }

    public record CustomAssetDefinition(
            String itemType,
            String naturalKeyField,
            List<String> sensitiveFieldNames,
            Map<String, FieldMapping> columns
    ) {
    }

    public record FieldMapping(String glpiField, FieldResolverType resolverType) {
    }

    public enum FieldResolverType {
        DIRECT,
        NATURAL_KEY,
        USER_LOGIN,
        STATE_LABEL,
        LOCATION_LABEL,
        DATE,
        SENSITIVE
    }
}
