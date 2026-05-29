package com.devgomes.glpi_integration.sync;

import com.devgomes.glpi_integration.client.GlpiApiException;
import com.devgomes.glpi_integration.config.GlpiCustomAssetsProperties;
import com.devgomes.glpi_integration.config.GlpiSyncProperties;
import com.devgomes.glpi_integration.service.GlpiIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class GenericItemSyncBatchService {

    private static final Logger log = LoggerFactory.getLogger(GenericItemSyncBatchService.class);

    private final CustomAssetSpreadsheetReader spreadsheetReader;
    private final AssetTypeRegistry assetTypeRegistry;
    private final GlpiIntegrationService glpiIntegrationService;
    private final GlpiSyncProperties syncProperties;

    public GenericItemSyncBatchService(
            CustomAssetSpreadsheetReader spreadsheetReader,
            AssetTypeRegistry assetTypeRegistry,
            GlpiIntegrationService glpiIntegrationService,
            GlpiSyncProperties syncProperties) {
        this.spreadsheetReader = spreadsheetReader;
        this.assetTypeRegistry = assetTypeRegistry;
        this.glpiIntegrationService = glpiIntegrationService;
        this.syncProperties = syncProperties;
    }

    public SyncReport syncFromFile(String assetKey, Path file) throws IOException {
        return processRows(assetKey, file.toString(), spreadsheetReader.read(file, assetKey), false);
    }

    public SyncReport validateFromFile(String assetKey, Path file) throws IOException {
        return processRows(assetKey, file.toString(), spreadsheetReader.read(file, assetKey), true);
    }

    public SyncReport processRows(String assetKey, String source, List<CustomAssetRow> rows, boolean dryRun) {
        GlpiCustomAssetsProperties.CustomAssetDefinition definition = assetTypeRegistry.get(assetKey);
        log.info("{} {} de {} linhas ({}) a partir de {}",
                dryRun ? "Validando" : "Sincronizando",
                assetKey,
                rows.size(),
                definition.itemType(),
                source);

        glpiIntegrationService.initSession();
        String range = syncProperties.getLookupRange();
        SyncLookupIndexes indexes = glpiIntegrationService.buildSyncLookupIndexes(range);
        Map<String, Integer> naturalKeyIndex = glpiIntegrationService.buildFieldValueIndex(
                definition.itemType(), definition.naturalKeyField(), range);
        Set<String> sensitiveKeys = new HashSet<>(definition.sensitiveFieldNames());

        List<SyncLineResult> results = new ArrayList<>();
        int success = 0;
        int failure = 0;

        for (CustomAssetRow row : rows) {
            try {
                ResolvedAssetTarget target = CustomAssetTargetResolver.resolve(row, definition, naturalKeyIndex);
                Map<String, Object> fields = SyncFieldResolver.resolveCustomAssetFields(assetKey, row, definition, indexes);
                if (fields.isEmpty()) {
                    throw new IllegalArgumentException("Linha " + row.lineNumber() + ": nenhum campo para enviar ao GLPI");
                }
                ensureNameForCreate(target, row, definition, fields);
                if (dryRun) {
                    Map<String, Object> masked = SensitiveDataMasker.maskFields(fields, sensitiveKeys);
                    String action = target.create() ? "would_create" : "would_update id=" + target.itemId();
                    results.add(new SyncLineResult(row.lineNumber(), target.itemId(), true,
                            action + " (" + target.matchedBy() + "): " + masked));
                } else if (target.create()) {
                    int newId = glpiIntegrationService.createItem(definition.itemType(), fields);
                    results.add(new SyncLineResult(row.lineNumber(), newId, true,
                            "OK (criado id=" + newId + ")"));
                } else {
                    int itemId = target.itemId();
                    try {
                        applyUpdate(assetKey, definition, itemId, fields);
                        results.add(new SyncLineResult(row.lineNumber(), itemId, true,
                                "OK (atualizado id=" + itemId + ", " + target.matchedBy() + ")"));
                    } catch (GlpiApiException ex) {
                        if (retryWithoutSensitiveFields(assetKey, definition, itemId, fields, ex)) {
                            results.add(new SyncLineResult(row.lineNumber(), itemId, true,
                                    "OK (atualizado id=" + itemId + ", sem campos de senha)"));
                        } else {
                            String partial = retryFieldByField(assetKey, definition, itemId, fields, ex);
                            if (partial != null) {
                                results.add(new SyncLineResult(row.lineNumber(), itemId, true, partial));
                            } else {
                                throw ex;
                            }
                        }
                    }
                }
                success++;
            } catch (GlpiApiException ex) {
                String message = "GLPI " + ex.getStatusCode().value() + ": " + ex.getMessage();
                if (message.contains("ERROR_GLPI_UPDATE")) {
                    message += " — use POST /api/custom-assets/" + assetKey + "/items/{id}/update-probe "
                            + "ou habilite «Atualizar todos» em Configuração → Ativos → definição → Perfis.";
                } else if (message.contains("ERROR_GLPI_ADD")) {
                    message += " — habilite «Criar» no perfil da definição do ativo (aba Perfis).";
                }
                results.add(new SyncLineResult(row.lineNumber(), row.glpiId(), false, message));
                failure++;
                log.warn("Linha {}: falha — {}", row.lineNumber(), message);
            } catch (Exception ex) {
                results.add(new SyncLineResult(row.lineNumber(), row.glpiId(), false, ex.getMessage()));
                failure++;
                log.warn("Linha {}: falha — {}", row.lineNumber(), ex.getMessage());
            }
            throttle();
        }

        log.info("{} {} concluída: {} sucesso, {} falha(s)",
                dryRun ? "Validação" : "Sincronização", assetKey, success, failure);
        return new SyncReport(source, rows.size(), success, failure, results);
    }

    private static void ensureNameForCreate(
            ResolvedAssetTarget target,
            CustomAssetRow row,
            GlpiCustomAssetsProperties.CustomAssetDefinition definition,
            Map<String, Object> fields) {
        if (!target.create()) {
            return;
        }
        if (fields.containsKey("name")) {
            return;
        }
        String name = CustomAssetNaturalKeySupport.displayNameForCreate(row, definition);
        if (name != null && !name.isBlank()) {
            fields.put("name", name.trim());
        }
    }

    private void applyUpdate(
            String assetKey,
            GlpiCustomAssetsProperties.CustomAssetDefinition definition,
            int itemId,
            Map<String, Object> fields) {
        glpiIntegrationService.updateItem(definition.itemType(), itemId, fields);
    }

    private String retryFieldByField(
            String assetKey,
            GlpiCustomAssetsProperties.CustomAssetDefinition definition,
            int itemId,
            Map<String, Object> fields,
            GlpiApiException original) {
        if (original.getMessage() == null || !original.getMessage().contains("ERROR_GLPI_UPDATE")) {
            return null;
        }
        Map<String, Object> withoutSensitive = stripSensitive(assetKey, definition, fields);
        List<String> applied = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        for (Map.Entry<String, Object> entry : withoutSensitive.entrySet()) {
            Map<String, Object> single = Map.of(entry.getKey(), entry.getValue());
            try {
                glpiIntegrationService.updateItem(definition.itemType(), itemId, single);
                applied.add(entry.getKey());
            } catch (GlpiApiException ex) {
                failed.add(entry.getKey() + ": " + ex.getMessage());
                log.warn("Campo {} rejeitado no PUT id={}: {}", entry.getKey(), itemId, ex.getMessage());
            }
        }

        if (applied.isEmpty()) {
            return null;
        }
        if (!failed.isEmpty()) {
            log.warn("PUT id={}: aplicados={}, falharam={}", itemId, applied, failed);
            return "OK parcial: aplicados=" + applied + ", falharam=" + failed;
        }
        return "OK (atualização campo a campo)";
    }

    private Map<String, Object> stripSensitive(
            String assetKey,
            GlpiCustomAssetsProperties.CustomAssetDefinition definition,
            Map<String, Object> fields) {
        Map<String, Object> withoutSensitive = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            if (!definition.sensitiveFieldNames().contains(entry.getKey())
                    && !assetTypeRegistry.isSensitiveField(assetKey, entry.getKey())) {
                withoutSensitive.put(entry.getKey(), entry.getValue());
            }
        }
        return withoutSensitive;
    }

    private boolean retryWithoutSensitiveFields(
            String assetKey,
            GlpiCustomAssetsProperties.CustomAssetDefinition definition,
            int itemId,
            Map<String, Object> fields,
            GlpiApiException original) {
        if (original.getMessage() == null || !original.getMessage().contains("ERROR_GLPI_UPDATE")) {
            return false;
        }
        Map<String, Object> withoutSensitive = stripSensitive(assetKey, definition, fields);
        if (withoutSensitive.size() == fields.size() || withoutSensitive.isEmpty()) {
            return false;
        }
        log.warn("Retry update id={} sem campos de senha ({} campo(s))", itemId, withoutSensitive.size());
        glpiIntegrationService.updateItem(definition.itemType(), itemId, withoutSensitive);
        return true;
    }

    private void throttle() {
        long delay = syncProperties.getDelayMs();
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
