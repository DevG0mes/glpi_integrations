package com.devgomes.glpi_integration.service;

import com.devgomes.glpi_integration.client.GlpiApiClient;
import com.devgomes.glpi_integration.client.GlpiApiException;
import com.devgomes.glpi_integration.client.GlpiItemTypePath;
import com.devgomes.glpi_integration.dto.ComputerListResponse;
import com.devgomes.glpi_integration.dto.ComputerUpdateRequest;
import com.devgomes.glpi_integration.dto.IdNameItem;
import com.devgomes.glpi_integration.session.GlpiSessionManager;
import com.devgomes.glpi_integration.sync.GlpiFieldFilter;
import com.devgomes.glpi_integration.sync.SyncFieldResolver;
import com.devgomes.glpi_integration.sync.SyncLookupIndexes;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class GlpiIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(GlpiIntegrationService.class);

    private final GlpiApiClient apiClient;
    private final GlpiSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    public GlpiIntegrationService(
            GlpiApiClient apiClient,
            GlpiSessionManager sessionManager,
            ObjectMapper objectMapper) {
        this.apiClient = apiClient;
        this.sessionManager = sessionManager;
        this.objectMapper = objectMapper;
    }

    /** Obtém sessão existente ou abre uma nova (sem invalidar sessão ativa). */
    public String ensureSession() {
        return sessionManager.getSessionToken();
    }

    /** Força nova sessão (útil para teste de conexão na subida). */
    public String initSession() {
        log.info("Abrindo nova sessão GLPI");
        return sessionManager.refreshSession();
    }

    public ComputerListResponse listComputers(String range, boolean expandDropdowns) {
        return listGlpiItems("Computer", range, expandDropdowns);
    }

    public List<IdNameItem> listComputerIdAndNames(String range) {
        return listComputers(range, false).items().stream()
                .map(this::toIdNameItem)
                .filter(item -> item != null)
                .toList();
    }

    /** Mapa name (normalizado) → id do Computer, para resolver linhas do CSV por id_ativo. */
    public Map<String, Integer> buildComputerNameIndex(String range) {
        Map<String, Integer> index = new HashMap<>();
        for (IdNameItem item : listComputerIdAndNames(range)) {
            if (item.name() != null && !item.name().isBlank()) {
                index.put(normalizeNameKey(item.name()), item.id());
            }
        }
        return index;
    }

    public static String normalizeNameKey(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    public ComputerListResponse listComputerModels(String range) {
        String effectiveRange = (range == null || range.isBlank()) ? "0-999" : range;
        log.info("Listando ComputerModel GLPI (range={})", effectiveRange);
        return listGlpiItems("ComputerModel", effectiveRange, false);
    }

    public List<IdNameItem> listComputerModelIdAndNames(String range) {
        return listComputerModels(range).items().stream()
                .map(this::toIdNameItem)
                .filter(item -> item != null)
                .toList();
    }

    public List<IdNameItem> listUserIdAndLogins(String range) {
        return listGlpiItems("User", range, false).items().stream()
                .map(this::toIdNameItem)
                .filter(item -> item != null)
                .toList();
    }

    public List<IdNameItem> listStateIdAndNames(String range) {
        return listGlpiItems("State", range, false).items().stream()
                .map(this::toIdNameItem)
                .filter(item -> item != null)
                .toList();
    }

    /** Mapa login (normalizado) → id do User. */
    public Map<String, Integer> buildUserLoginIndex(String range) {
        Map<String, Integer> index = new HashMap<>();
        for (IdNameItem item : listUserIdAndLogins(range)) {
            if (item.name() != null && !item.name().isBlank()) {
                index.put(normalizeNameKey(item.name()), item.id());
            }
        }
        return index;
    }

    /** Mapa nome do status (normalizado, sem acento) → id do State. */
    public Map<String, Integer> buildStateLabelIndex(String range) {
        return buildLabelIndex(listStateIdAndNames(range));
    }

    public List<IdNameItem> listLocationIdAndNames(String range) {
        return listGlpiItems("Location", range, false).items().stream()
                .map(this::toIdNameItem)
                .filter(item -> item != null)
                .toList();
    }

    public List<IdNameItem> listGroupIdAndNames(String range) {
        return listGlpiItems("Group", range, false).items().stream()
                .map(this::toIdNameItem)
                .filter(item -> item != null)
                .toList();
    }

    public List<IdNameItem> listComputerTypeIdAndNames(String range) {
        return listGlpiItems("ComputerType", range, false).items().stream()
                .map(this::toIdNameItem)
                .filter(item -> item != null)
                .toList();
    }

    public List<IdNameItem> listManufacturerIdAndNames(String range) {
        return listGlpiItems("Manufacturer", range, false).items().stream()
                .map(this::toIdNameItem)
                .filter(item -> item != null)
                .toList();
    }

    public Map<String, Integer> buildLocationLabelIndex(String range) {
        return buildLabelIndex(listLocationIdAndNames(range));
    }

    public Map<String, Integer> buildGroupLabelIndex(String range) {
        return buildLabelIndex(listGroupIdAndNames(range));
    }

    public Map<String, Integer> buildComputerTypeLabelIndex(String range) {
        return buildLabelIndex(listComputerTypeIdAndNames(range));
    }

    public Map<String, Integer> buildManufacturerLabelIndex(String range) {
        return buildLabelIndex(listManufacturerIdAndNames(range));
    }

    public SyncLookupIndexes buildSyncLookupIndexes(String range) {
        return new SyncLookupIndexes(
                buildUserLoginIndex(range),
                buildStateLabelIndex(range),
                buildLocationLabelIndex(range),
                buildGroupLabelIndex(range),
                buildComputerTypeLabelIndex(range),
                buildManufacturerLabelIndex(range)
        );
    }

    public List<IdNameItem> listCustomAssetIdAndNames(String itemType, String range) {
        return listGlpiItems(itemType, range, false).items().stream()
                .map(this::toIdNameItem)
                .filter(item -> item != null)
                .toList();
    }

    public ComputerListResponse listCustomAssetItems(String itemType, String range, boolean expandDropdowns) {
        return listGlpiItems(itemType, range, expandDropdowns);
    }

    /** Linhas brutas de um itemtype GLPI (ex. definições de ativo). */
    public List<Map<String, Object>> listGlpiItemRows(String itemType, String range) {
        return listGlpiItems(itemType, range, false).items();
    }

    public Map<String, Integer> buildFieldValueIndex(String itemType, String fieldName, String range) {
        Map<String, Integer> index = new HashMap<>();
        for (Map<String, Object> item : listGlpiItems(itemType, range, false).items()) {
            Object id = item.get("id");
            Object value = item.get(fieldName);
            if (id == null || value == null) {
                continue;
            }
            int parsedId = id instanceof Number number ? number.intValue() : Integer.parseInt(id.toString());
            index.put(SyncFieldResolver.normalizeLabelKey(value.toString()), parsedId);
        }
        return index;
    }

    public Map<String, Object> getItem(String itemType, int itemId, boolean expandDropdowns) {
        String mutationType = GlpiItemTypePath.mutationItemType(itemType);
        log.info("Buscando {} id={} (mutation={})", itemType, itemId, mutationType);
        String json = sessionManager.executeWithSession(token -> {
            try {
                return apiClient.getItem(token, mutationType, itemId, expandDropdowns);
            } catch (GlpiApiException ex) {
                if (!mutationType.equals(itemType) && isNotFound(ex)) {
                    return apiClient.getItem(token, itemType, itemId, expandDropdowns);
                }
                throw ex;
            }
        });
        return parseItemObject(json);
    }

    public void updateItem(String itemType, int itemId, Map<String, Object> fields) {
        updateItem(itemType, itemId, fields, true);
    }

    public void updateItem(String itemType, int itemId, Map<String, Object> fields, boolean filterByExistingItem) {
        if (fields.isEmpty()) {
            throw new IllegalArgumentException("Nenhum campo informado para atualização de " + itemType + " " + itemId);
        }
        Map<String, Object> payload = fields;
        if (filterByExistingItem) {
            payload = filterPayloadAgainstGlpiItem(itemType, itemId, fields);
            if (payload.isEmpty()) {
                throw new IllegalArgumentException(
                        "Nenhum campo reconhecido pelo GLPI para atualização de " + itemType + " " + itemId
                                + ". Use GET /api/custom-assets/{key}/items/" + itemId + " para ver chaves válidas.");
            }
        }

        String mutationType = GlpiItemTypePath.mutationItemType(itemType);
        log.info("Atualizando {} id={} com {} campo(s) via PUT em {}: {}",
                itemType, itemId, payload.size(), mutationType, summarizeFieldKeys(payload));
        Map<String, Object> finalPayload = payload;
        sessionManager.runWithSession(token -> {
            if (mutationType.equals(itemType)) {
                apiClient.updateItem(token, itemType, itemId, finalPayload);
                return;
            }
            try {
                apiClient.updateItem(token, mutationType, itemId, finalPayload);
            } catch (GlpiApiException first) {
                if (isNotFound(first)) {
                    log.warn("PUT em {} retornou 404, tentando {}", mutationType, itemType);
                    apiClient.updateItem(token, itemType, itemId, finalPayload);
                } else {
                    throw first;
                }
            }
        });
    }

    private Map<String, Object> filterPayloadAgainstGlpiItem(
            String itemType,
            int itemId,
            Map<String, Object> fields) {
        try {
            Map<String, Object> existing = getItem(itemType, itemId, false);
            GlpiFieldFilter.FilterResult filtered = GlpiFieldFilter.retainFieldsKnownToItem(fields, existing);
            if (!filtered.droppedFieldNames().isEmpty()) {
                log.warn("Campos ignorados no PUT id={} (não existem no GET GLPI): {}",
                        itemId, filtered.droppedFieldNames());
            }
            return filtered.fields();
        } catch (Exception ex) {
            log.warn("Não foi possível filtrar campos via GET id={}: {}", itemId, ex.getMessage());
            return fields;
        }
    }

    private static boolean isNotFound(GlpiApiException ex) {
        return ex.getStatusCode() != null && ex.getStatusCode().value() == 404;
    }

    public int createItem(String itemType, Map<String, Object> fields) {
        if (fields.isEmpty()) {
            throw new IllegalArgumentException("Nenhum campo informado para criação de " + itemType);
        }
        String mutationType = GlpiItemTypePath.mutationItemType(itemType);
        log.info("Criando {} (mutation={}) com {} campo(s): {}",
                itemType, mutationType, fields.size(), summarizeFieldKeys(fields));
        return sessionManager.executeWithSession(token ->
                apiClient.createItem(token, mutationType, fields));
    }

    private static Map<String, Object> summarizeFieldKeys(Map<String, Object> fields) {
        Map<String, Object> summary = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            String key = entry.getKey();
            if (key == null) {
                continue;
            }
            if (key.startsWith("custom_") && key.contains("vencimento")) {
                summary.put(key, entry.getValue());
            } else {
                summary.put(key, "<set>");
            }
        }
        return summary;
    }

    private Map<String, Integer> buildLabelIndex(List<IdNameItem> items) {
        Map<String, Integer> index = new HashMap<>();
        for (IdNameItem item : items) {
            if (item.name() != null && !item.name().isBlank()) {
                index.put(SyncFieldResolver.normalizeLabelKey(item.name()), item.id());
            }
        }
        return index;
    }

    private ComputerListResponse listGlpiItems(String itemType, String range, boolean expandDropdowns) {
        String effectiveRange = (range == null || range.isBlank()) ? "0-999" : range;

        // CORREÇÃO: Aplicando o mutationType para converter as barras invertidas corretamente antes de enviar à API
        String mutationType = GlpiItemTypePath.mutationItemType(itemType);

        log.info("Listando {} (mutation={}) GLPI (range={}, expandDropdowns={})", itemType, mutationType, effectiveRange, expandDropdowns);

        ResponseEntity<String> response = sessionManager.executeWithSession(token -> {
            try {
                return apiClient.listItems(token, mutationType, effectiveRange, expandDropdowns);
            } catch (GlpiApiException ex) {
                if (!mutationType.equals(itemType) && isNotFound(ex)) {
                    return apiClient.listItems(token, itemType, effectiveRange, expandDropdowns);
                }
                throw ex;
            }
        });

        List<Map<String, Object>> items = parseJsonArray(response.getBody());
        String contentRange = response.getHeaders().getFirst("Content-Range");

        return new ComputerListResponse(
                effectiveRange,
                contentRange,
                items.size(),
                items
        );
    }

    public Map<String, Object> getComputer(int computerId, boolean expandDropdowns) {
        log.info("Buscando Computer id={}", computerId);
        String json = sessionManager.executeWithSession(token ->
                apiClient.getComputer(token, computerId, expandDropdowns));
        return parseComputerObject(json);
    }

    public void updateComputer(int computerId, ComputerUpdateRequest request) {
        var fields = request.toInputMap();
        if (fields.isEmpty()) {
            throw new IllegalArgumentException("Nenhum campo informado para atualização do Computer " + computerId);
        }
        log.info("Atualizando Computer id={} com {} campo(s)", computerId, fields.size());
        sessionManager.runWithSession(token ->
                apiClient.updateComputer(token, computerId, fields));
    }

    private IdNameItem toIdNameItem(Map<String, Object> row) {
        Object id = row.get("id");
        if (id == null) {
            return null;
        }
        int parsedId = id instanceof Number number ? number.intValue() : Integer.parseInt(id.toString());

        Object name = row.get("name");

        // CORREÇÃO: Fallback inteligente para quando o GLPI não envia o campo "name" preenchido
        if (name == null || name.toString().isBlank()) {
            if (row.containsKey("custom_email")) {
                name = row.get("custom_email");
            } else if (row.containsKey("custom_iccid")) {
                name = row.get("custom_iccid");
            } else if (row.containsKey("custom_imei")) {
                name = row.get("custom_imei");
            }
        }

        String parsedName = name != null ? name.toString() : "";
        return new IdNameItem(parsedId, parsedName);
    }

    private List<Map<String, Object>> parseJsonArray(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }

        // Proteção contra o "Fantasma do HTML": estoura erro claro se o GLPI retornar 404 como página web
        if (json.trim().startsWith("<")) {
            throw new IllegalStateException("O GLPI retornou uma página HTML em vez de JSON. " +
                    "A rota mapeada está incorreta. Resposta parcial: " +
                    json.substring(0, Math.min(json.length(), 200)));
        }

        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Resposta inesperada ao listar itens GLPI: " + json, ex);
        }
    }

    private Map<String, Object> parseComputerObject(String json) {
        return parseItemObject(json);
    }

    private Map<String, Object> parseItemObject(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalStateException("Resposta vazia ao buscar item GLPI");
        }

        if (json.trim().startsWith("<")) {
            throw new IllegalStateException("O GLPI retornou HTML. Rota incorreta. Resposta: " +
                    json.substring(0, Math.min(json.length(), 200)));
        }

        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Resposta inesperada ao buscar item GLPI: " + json, ex);
        }
    }
}
