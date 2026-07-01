package com.devgomes.glpi_integration.service;

import com.devgomes.glpi_integration.config.GlpiCustomAssetsProperties;
import com.devgomes.glpi_integration.config.GlpiSyncProperties;
import com.devgomes.glpi_integration.sync.AssetTypeRegistry;
import com.devgomes.glpi_integration.sync.CustomAssetNaturalKeySupport;
import com.devgomes.glpi_integration.sync.CustomAssetRow;
import com.devgomes.glpi_integration.sync.CustomAssetTargetResolver;
import com.devgomes.glpi_integration.sync.ResolvedAssetTarget;
import com.devgomes.glpi_integration.sync.SyncFieldResolver;
import com.devgomes.glpi_integration.sync.SyncLookupIndexes;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cria e atualiza itens de ativos customizados via JSON (mesmas colunas da planilha).
 */
@Service
public class CustomAssetItemWriteService {

    private final AssetTypeRegistry assetTypeRegistry;
    private final GlpiIntegrationService glpiIntegrationService;
    private final GlpiSyncProperties syncProperties;

    public CustomAssetItemWriteService(
            AssetTypeRegistry assetTypeRegistry,
            GlpiIntegrationService glpiIntegrationService,
            GlpiSyncProperties syncProperties) {
        this.assetTypeRegistry = assetTypeRegistry;
        this.glpiIntegrationService = glpiIntegrationService;
        this.syncProperties = syncProperties;
    }

    public Map<String, Object> create(String assetKey, Map<String, ?> body) {
        GlpiCustomAssetsProperties.CustomAssetDefinition definition = assetTypeRegistry.get(assetKey);
        glpiIntegrationService.initSession();
        Map<String, String> columns = toColumnMap(body, definition);
        CustomAssetRow row = new CustomAssetRow(1, 0, null, columns);
        Map<String, Integer> naturalKeyIndex = glpiIntegrationService.buildFieldValueIndex(
                definition.itemType(), definition.naturalKeyField(), syncProperties.getLookupRange());
        ResolvedAssetTarget target = CustomAssetTargetResolver.resolve(row, definition, naturalKeyIndex);
        if (!target.create()) {
            throw new IllegalArgumentException(
                    "Item já existe para " + assetKey + " (id=" + target.itemId() + ", " + target.matchedBy()
                            + "). Use PUT para atualizar.");
        }
        Map<String, Object> fields = resolveFields(assetKey, row, definition);
        ensureNameForCreate(row, definition, fields);
        int newId = glpiIntegrationService.createItem(definition.itemType(), fields);
        applyPostCreateDateUpdate(definition, newId, fields);
        return responseMap(assetKey, definition, newId, "created", fields);
    }

    public Map<String, Object> update(String assetKey, int itemId, Map<String, ?> body) {
        GlpiCustomAssetsProperties.CustomAssetDefinition definition = assetTypeRegistry.get(assetKey);
        glpiIntegrationService.initSession();
        Map<String, String> columns = toColumnMap(body, definition);
        CustomAssetRow row = new CustomAssetRow(1, itemId, null, columns);
        Map<String, Object> fields = resolveFields(assetKey, row, definition);
        if (fields.isEmpty()) {
            throw new IllegalArgumentException("Nenhum campo reconhecido no body. Colunas: "
                    + definition.columns().keySet());
        }
        glpiIntegrationService.updateItem(definition.itemType(), itemId, fields);
        return responseMap(assetKey, definition, itemId, "updated", fields);
    }

    private Map<String, Object> resolveFields(
            String assetKey,
            CustomAssetRow row,
            GlpiCustomAssetsProperties.CustomAssetDefinition definition) {
        SyncLookupIndexes indexes = glpiIntegrationService.buildSyncLookupIndexes(syncProperties.getLookupRange());
        Map<String, Object> fields = SyncFieldResolver.resolveCustomAssetFields(assetKey, row, definition, indexes);
        if (fields.isEmpty()) {
            throw new IllegalArgumentException("Nenhum campo válido para enviar ao GLPI");
        }
        return fields;
    }

    private static void ensureNameForCreate(
            CustomAssetRow row,
            GlpiCustomAssetsProperties.CustomAssetDefinition definition,
            Map<String, Object> fields) {
        if (fields.containsKey("name")) {
            return;
        }
        String name = CustomAssetNaturalKeySupport.displayNameForCreate(row, definition);
        if (name != null && !name.isBlank()) {
            fields.put("name", name.trim());
        }
    }

    private void applyPostCreateDateUpdate(
            GlpiCustomAssetsProperties.CustomAssetDefinition definition,
            int itemId,
            Map<String, Object> fields) {
        Map<String, Object> dateFields = new LinkedHashMap<>();
        for (GlpiCustomAssetsProperties.FieldMapping mapping : definition.columns().values()) {
            if (mapping.resolverType() == GlpiCustomAssetsProperties.FieldResolverType.DATE
                    && fields.containsKey(mapping.glpiField())) {
                dateFields.put(mapping.glpiField(), fields.get(mapping.glpiField()));
            }
        }
        if (dateFields.isEmpty()) {
            return;
        }
        glpiIntegrationService.updateItem(definition.itemType(), itemId, dateFields);
    }

    private static Map<String, String> toColumnMap(
            Map<String, ?> body,
            GlpiCustomAssetsProperties.CustomAssetDefinition definition) {
        if (body == null || body.isEmpty()) {
            throw new IllegalArgumentException("Body JSON vazio");
        }
        Map<String, String> columns = new LinkedHashMap<>();
        Map<String, String> glpiToColumn = new LinkedHashMap<>();
        definition.columns().forEach((col, mapping) -> glpiToColumn.put(mapping.glpiField(), col));

        for (Map.Entry<String, ?> entry : body.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            String key = entry.getKey().trim();
            String value = entry.getValue().toString().trim();
            if (value.isEmpty()) {
                continue;
            }
            if (definition.columns().containsKey(key)) {
                columns.put(key, value);
            } else if (glpiToColumn.containsKey(key)) {
                columns.put(glpiToColumn.get(key), value);
            }
        }
        return columns;
    }

    private static Map<String, Object> responseMap(
            String assetKey,
            GlpiCustomAssetsProperties.CustomAssetDefinition definition,
            int itemId,
            String status,
            Map<String, Object> fieldsSent) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("assetKey", assetKey);
        body.put("itemType", definition.itemType());
        body.put("itemId", itemId);
        body.put("status", status);
        body.put("fieldsSent", fieldsSent);
        return body;
    }
}
