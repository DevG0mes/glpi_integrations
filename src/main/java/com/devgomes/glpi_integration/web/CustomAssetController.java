package com.devgomes.glpi_integration.web;

import com.devgomes.glpi_integration.client.GlpiItemTypePath;
import com.devgomes.glpi_integration.config.GlpiCustomAssetsProperties;
import com.devgomes.glpi_integration.dto.CustomAssetConfigItem;
import com.devgomes.glpi_integration.dto.IdNameItem;
import com.devgomes.glpi_integration.service.CustomAssetItemTypeDiscoveryService;
import com.devgomes.glpi_integration.service.CustomAssetUpdateProbeService;
import com.devgomes.glpi_integration.service.GlpiAssetDefinitionCatalog;
import com.devgomes.glpi_integration.service.GlpiIntegrationService;
import com.devgomes.glpi_integration.sync.AssetTypeRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Ativos customizados", description = "Starlink, Chip, Celular. assetKey: starlink | chip | celular")
@RestController
@RequestMapping("/api/custom-assets")
public class CustomAssetController {

    private final GlpiIntegrationService glpiIntegrationService;
    private final AssetTypeRegistry assetTypeRegistry;
    private final CustomAssetItemTypeDiscoveryService discoveryService;
    private final GlpiAssetDefinitionCatalog definitionCatalog;
    private final CustomAssetUpdateProbeService updateProbeService;

    public CustomAssetController(
            GlpiIntegrationService glpiIntegrationService,
            AssetTypeRegistry assetTypeRegistry,
            CustomAssetItemTypeDiscoveryService discoveryService,
            GlpiAssetDefinitionCatalog definitionCatalog,
            CustomAssetUpdateProbeService updateProbeService) {
        this.glpiIntegrationService = glpiIntegrationService;
        this.assetTypeRegistry = assetTypeRegistry;
        this.discoveryService = discoveryService;
        this.definitionCatalog = definitionCatalog;
        this.updateProbeService = updateProbeService;
    }

    /**
     * Testa vários itemtypes candidatos no GLPI 11 e devolve o que funcionou
     * + linhas prontas para application-local.properties.
     */
    @GetMapping("/discover")
    public ResponseEntity<Map<String, Object>> discoverAll() {
        return ResponseEntity.ok(discoveryService.discoverAll());
    }

    @GetMapping("/{assetKey}/discover")
    public ResponseEntity<Map<String, Object>> discoverOne(
            @PathVariable String assetKey,
            @RequestParam(required = false) String systemName) {
        if (systemName != null && !systemName.isBlank()) {
            return ResponseEntity.ok(discoveryService.discoverWithSystemName(assetKey, systemName));
        }
        return ResponseEntity.ok(discoveryService.discoverOne(assetKey));
    }

    /** Lista definições de ativo cadastradas no GLPI (nome do sistema + rótulo). */
    @GetMapping("/definitions")
    public ResponseEntity<Map<String, Object>> listDefinitions() {
        glpiIntegrationService.initSession();
        var result = definitionCatalog.fetchDefinitionsWithDiagnostics();
        var rows = result.definitions().stream()
                .map(definitionCatalog::toSummary)
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("count", rows.size());
        body.put("definitions", rows);
        body.put("listAttempts", result.listAttempts());
        if (result.warning() != null) {
            body.put("warning", result.warning());
        }
        body.put("hint", "Copie system_name → GET /api/custom-assets/starlink/discover?systemName=VALOR");
        body.put("manualTestUrlExample",
                "GET {baseUrl}/Glpi%5CAsset%5CAssetDefinition/SEU_SYSTEM_NAME/?range=0-0");
        return ResponseEntity.ok(body);
    }

    @GetMapping("/config")
    public ResponseEntity<List<CustomAssetConfigItem>> config() {
        List<CustomAssetConfigItem> items = new ArrayList<>();
        for (String key : assetTypeRegistry.keys()) {
            GlpiCustomAssetsProperties.CustomAssetDefinition def = assetTypeRegistry.get(key);
            Map<String, String> columnMap = new LinkedHashMap<>();
            def.columns().forEach((col, mapping) -> columnMap.put(col, mapping.glpiField()));
            items.add(new CustomAssetConfigItem(
                    key,
                    def.itemType(),
                    GlpiItemTypePath.encode(def.itemType()),
                    !GlpiItemTypePath.isPlaceholder(def.itemType()),
                    def.naturalKeyField(),
                    List.copyOf(def.columns().keySet()),
                    columnMap
            ));
        }
        return ResponseEntity.ok(items);
    }

    @Operation(summary = "Listar id + nome dos itens", description = "Ex.: GET .../starlink/summary")
    @GetMapping("/{assetKey}/summary")
    public ResponseEntity<List<IdNameItem>> summary(
            @Parameter(description = "starlink, chip ou celular") @PathVariable String assetKey,
            @RequestParam(defaultValue = "0-999") String range) {
        var definition = assetTypeRegistry.get(assetKey);
        return ResponseEntity.ok(glpiIntegrationService.listCustomAssetIdAndNames(definition.itemType(), range));
    }

    /** Retorna o item GLPI bruto (útil para conferir nomes de campos custom_*). */
    @GetMapping("/{assetKey}/items/{itemId}")
    public ResponseEntity<Map<String, Object>> getItem(
            @PathVariable String assetKey,
            @PathVariable int itemId,
            @RequestParam(defaultValue = "false") boolean expandDropdowns) {
        var definition = assetTypeRegistry.get(assetKey);
        glpiIntegrationService.initSession();
        Map<String, Object> item = glpiIntegrationService.getItem(definition.itemType(), itemId, expandDropdowns);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("assetKey", assetKey);
        body.put("itemType", definition.itemType());
        body.put("mutationItemType", GlpiItemTypePath.mutationItemType(definition.itemType()));
        body.put("itemId", itemId);
        body.put("item", item);
        body.put("customFieldKeys", item.keySet().stream()
                .map(Object::toString)
                .filter(k -> k.startsWith("custom_"))
                .sorted()
                .toList());
        return ResponseEntity.ok(body);
    }

    /**
     * Testa PUT campo a campo (diagnóstico de ERROR_GLPI_UPDATE).
     * Body opcional: {@code {"name":"...", "custom_projeto":"...", "users_id":96}}.
     */
    @PostMapping("/{assetKey}/items/{itemId}/update-probe")
    public ResponseEntity<Map<String, Object>> updateProbe(
            @PathVariable String assetKey,
            @PathVariable int itemId,
            @RequestBody(required = false) Map<String, Object> fields) {
        return ResponseEntity.ok(updateProbeService.probe(assetKey, itemId, fields));
    }

    @GetMapping("/{assetKey}/probe")
    public ResponseEntity<Map<String, Object>> probe(@PathVariable String assetKey) {
        var definition = assetTypeRegistry.get(assetKey);
        try {
            glpiIntegrationService.initSession();
            var list = glpiIntegrationService.listCustomAssetIdAndNames(definition.itemType(), "0-0");
            return ResponseEntity.ok(Map.of(
                    "assetKey", assetKey,
                    "itemType", definition.itemType(),
                    "itemTypeEncoded", GlpiItemTypePath.encode(definition.itemType()),
                    "reachable", true,
                    "sampleCount", list.size()
            ));
        } catch (Exception ex) {
            return ResponseEntity.ok(Map.of(
                    "assetKey", assetKey,
                    "itemType", definition.itemType(),
                    "itemTypeEncoded", GlpiItemTypePath.encode(definition.itemType()),
                    "reachable", false,
                    "error", ex.getMessage()
            ));
        }
    }
}
