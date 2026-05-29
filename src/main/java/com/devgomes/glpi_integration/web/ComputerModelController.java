package com.devgomes.glpi_integration.web;

import com.devgomes.glpi_integration.dto.ComputerListResponse;
import com.devgomes.glpi_integration.dto.IdNameItem;
import com.devgomes.glpi_integration.service.GlpiIntegrationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Computer — modelos", description = "Modelos de computer no GLPI")
@RestController
@RequestMapping("/api/computer-models")
public class ComputerModelController {

    private final GlpiIntegrationService glpiIntegrationService;

    public ComputerModelController(GlpiIntegrationService glpiIntegrationService) {
        this.glpiIntegrationService = glpiIntegrationService;
    }

    /** Lista completa (campos crus do GLPI). */
    @GetMapping
    public ResponseEntity<ComputerListResponse> list(
            @RequestParam(defaultValue = "0-999") String range) {
        return ResponseEntity.ok(glpiIntegrationService.listComputerModels(range));
    }

    /** Somente id + name — ideal para achar o computermodels_id. */
    @GetMapping("/summary")
    public ResponseEntity<List<IdNameItem>> summary(
            @RequestParam(defaultValue = "0-999") String range) {
        return ResponseEntity.ok(glpiIntegrationService.listComputerModelIdAndNames(range));
    }
}
