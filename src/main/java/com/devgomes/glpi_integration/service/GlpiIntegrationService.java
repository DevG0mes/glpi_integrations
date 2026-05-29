package com.devgomes.glpi_integration.service;

import com.devgomes.glpi_integration.client.GlpiApiClient;
import com.devgomes.glpi_integration.dto.ComputerListResponse;
import com.devgomes.glpi_integration.dto.ComputerUpdateRequest;
import com.devgomes.glpi_integration.dto.IdNameItem;
import com.devgomes.glpi_integration.session.GlpiSessionManager;
import com.devgomes.glpi_integration.sync.SyncFieldResolver;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
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
        Map<String, Integer> index = new HashMap<>();
        for (IdNameItem item : listStateIdAndNames(range)) {
            if (item.name() != null && !item.name().isBlank()) {
                index.put(SyncFieldResolver.normalizeLabelKey(item.name()), item.id());
            }
        }
        return index;
    }

    private ComputerListResponse listGlpiItems(String itemType, String range, boolean expandDropdowns) {
        String effectiveRange = (range == null || range.isBlank()) ? "0-999" : range;
        log.info("Listando {} GLPI (range={}, expandDropdowns={})", itemType, effectiveRange, expandDropdowns);

        ResponseEntity<String> response = sessionManager.executeWithSession(token ->
                apiClient.listItems(token, itemType, effectiveRange, expandDropdowns));

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
        Object name = row.get("name");
        if (id == null) {
            return null;
        }
        int parsedId = id instanceof Number number ? number.intValue() : Integer.parseInt(id.toString());
        String parsedName = name != null ? name.toString() : "";
        return new IdNameItem(parsedId, parsedName);
    }

    private List<Map<String, Object>> parseJsonArray(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Resposta inesperada ao listar itens GLPI: " + json, ex);
        }
    }

    private Map<String, Object> parseComputerObject(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalStateException("Resposta vazia ao buscar Computer");
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Resposta inesperada ao buscar Computer: " + json, ex);
        }
    }
}
