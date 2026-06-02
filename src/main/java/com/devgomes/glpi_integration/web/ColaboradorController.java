package com.devgomes.glpi_integration.web;

import com.devgomes.glpi_integration.dto.ColaboradorWriteRequest;
import com.devgomes.glpi_integration.dto.ComputerListResponse;
import com.devgomes.glpi_integration.dto.IdNameItem;
import com.devgomes.glpi_integration.service.CustomAssetItemWriteService;
import com.devgomes.glpi_integration.service.GlpiIntegrationService;
import com.devgomes.glpi_integration.sync.AssetTypeRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Colaborador", description = "Ativo customizado GLPI — Colaborador (nome, departamento, email, ativo)")
@RestController
@RequestMapping("/api/colaboradores")
public class ColaboradorController {

    private static final String ASSET_KEY = "colaborador";

    private final GlpiIntegrationService glpiIntegrationService;
    private final AssetTypeRegistry assetTypeRegistry;
    private final CustomAssetItemWriteService itemWriteService;

    public ColaboradorController(
            GlpiIntegrationService glpiIntegrationService,
            AssetTypeRegistry assetTypeRegistry,
            CustomAssetItemWriteService itemWriteService) {
        this.glpiIntegrationService = glpiIntegrationService;
        this.assetTypeRegistry = assetTypeRegistry;
        this.itemWriteService = itemWriteService;
    }

    @Operation(summary = "Listar colaboradores (id + nome)")
    @GetMapping("/summary")
    public ResponseEntity<List<IdNameItem>> summary(
            @RequestParam(defaultValue = "0-999") String range) {
        var definition = assetTypeRegistry.get(ASSET_KEY);
        return ResponseEntity.ok(glpiIntegrationService.listCustomAssetIdAndNames(definition.itemType(), range));
    }

    @Operation(summary = "Listar colaboradores (campos completos)")
    @GetMapping
    public ResponseEntity<ComputerListResponse> list(
            @RequestParam(defaultValue = "0-999") String range,
            @RequestParam(defaultValue = "false") boolean expandDropdowns) {
        var definition = assetTypeRegistry.get(ASSET_KEY);
        return ResponseEntity.ok(
                glpiIntegrationService.listCustomAssetItems(definition.itemType(), range, expandDropdowns));
    }

    @Operation(summary = "Obter colaborador por e-mail (chave natural)")
    @GetMapping("/by-email/{email}")
    public ResponseEntity<Map<String, Object>> getByEmail(
            @PathVariable String email,
            @RequestParam(defaultValue = "false") boolean expandDropdowns) {
        var definition = assetTypeRegistry.get(ASSET_KEY);
        glpiIntegrationService.initSession();
        Map<String, Integer> index = glpiIntegrationService.buildFieldValueIndex(
                definition.itemType(), definition.naturalKeyField(), "0-9999");
        String key = com.devgomes.glpi_integration.sync.SyncFieldResolver.normalizeLabelKey(email);
        Integer id = index.get(key);
        if (id == null) {
            throw new IllegalArgumentException("Colaborador não encontrado para email: " + email);
        }
        return getById(id, expandDropdowns);
    }

    @Operation(summary = "Obter colaborador por id")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(
            @PathVariable int id,
            @RequestParam(defaultValue = "false") boolean expandDropdowns) {
        var definition = assetTypeRegistry.get(ASSET_KEY);
        glpiIntegrationService.initSession();
        Map<String, Object> item = glpiIntegrationService.getItem(definition.itemType(), id, expandDropdowns);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("assetKey", ASSET_KEY);
        body.put("itemType", definition.itemType());
        body.put("itemId", id);
        body.put("item", item);
        body.put("customFieldKeys", item.keySet().stream()
                .map(Object::toString)
                .filter(k -> k.startsWith("custom_"))
                .sorted()
                .toList());
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Criar colaborador", description = "Chave natural: email. Duplicata retorna erro — use PUT.")
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody ColaboradorWriteRequest request) {
        return ResponseEntity.ok(itemWriteService.create(ASSET_KEY, request.toColumnMap()));
    }

    @Operation(summary = "Atualizar colaborador")
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable int id,
            @RequestBody ColaboradorWriteRequest request) {
        return ResponseEntity.ok(itemWriteService.update(ASSET_KEY, id, request.toColumnMap()));
    }
}
