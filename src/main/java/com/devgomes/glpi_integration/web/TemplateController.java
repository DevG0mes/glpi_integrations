package com.devgomes.glpi_integration.web;

import com.devgomes.glpi_integration.config.GlpiCustomAssetsProperties;
import com.devgomes.glpi_integration.service.GlpiIntegrationService;
import com.devgomes.glpi_integration.sync.AssetTypeRegistry;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private static final List<String> COMPUTER_HEADERS = List.of(
            "id_ativo", "serial", "responsavel", "local", "grupo", "tipo", "fabricante", "nome", "observacao"
    );

    private final GlpiIntegrationService glpiIntegrationService;
    private final AssetTypeRegistry assetTypeRegistry;

    public TemplateController(
            GlpiIntegrationService glpiIntegrationService,
            AssetTypeRegistry assetTypeRegistry) {
        this.glpiIntegrationService = glpiIntegrationService;
        this.assetTypeRegistry = assetTypeRegistry;
    }

    @GetMapping("/{assetKey}.csv")
    public ResponseEntity<byte[]> downloadTemplate(@PathVariable String assetKey) {
        String normalizedKey = assetKey == null ? "" : assetKey.trim().toLowerCase();
        glpiIntegrationService.initSession();

        String csv = switch (normalizedKey) {
            case "computers" -> buildComputersTemplate();
            default -> buildCustomAssetTemplate(normalizedKey);
        };

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("template_" + normalizedKey + ".csv", StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok()
                .headers(headers)
                .body(("\ufeff" + csv).getBytes(StandardCharsets.UTF_8));
    }

    private String buildComputersTemplate() {
        List<List<String>> rows = new ArrayList<>();
        rows.add(COMPUTER_HEADERS);
        for (Map<String, Object> item : glpiIntegrationService.listComputerRows("0-999")) {
            rows.add(List.of(
                    value(item.get("id")),
                    value(item.get("serial")),
                    value(item.get("users_id")),
                    value(item.get("locations_id")),
                    value(item.get("groups_id")),
                    value(item.get("computertypes_id")),
                    value(item.get("manufacturers_id")),
                    value(item.get("name")),
                    value(item.get("comment"))
            ));
        }
        return toCsv(rows);
    }

    private String buildCustomAssetTemplate(String assetKey) {
        GlpiCustomAssetsProperties.CustomAssetDefinition definition = assetTypeRegistry.get(assetKey);
        Set<String> orderedColumns = new LinkedHashSet<>();
        orderedColumns.add("id_ativo");
        orderedColumns.addAll(definition.columns().keySet());
        List<String> headers = List.copyOf(orderedColumns);

        List<List<String>> rows = new ArrayList<>();
        rows.add(headers);
        for (Map<String, Object> item : glpiIntegrationService.listGlpiItemRows(definition.itemType(), "0-999")) {
            List<String> line = new ArrayList<>(headers.size());
            for (String header : headers) {
                if ("id_ativo".equals(header)) {
                    line.add(value(item.get("id")));
                    continue;
                }
                GlpiCustomAssetsProperties.FieldMapping mapping = definition.columns().get(header);
                line.add(mapping == null ? "" : value(item.get(mapping.glpiField())));
            }
            rows.add(line);
        }
        return toCsv(rows);
    }

    private String toCsv(List<List<String>> rows) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < rows.size(); i++) {
            if (i > 0) {
                builder.append('\n');
            }
            List<String> row = rows.get(i);
            for (int j = 0; j < row.size(); j++) {
                if (j > 0) {
                    builder.append(';');
                }
                builder.append(escapeCsv(row.get(j)));
            }
        }
        return builder.toString();
    }

    private String escapeCsv(String value) {
        String safe = value == null ? "" : value;
        boolean needsQuotes = safe.contains(";") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r");
        if (!needsQuotes) {
            return safe;
        }
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private String value(Object raw) {
        return raw == null ? "" : raw.toString();
    }
}
