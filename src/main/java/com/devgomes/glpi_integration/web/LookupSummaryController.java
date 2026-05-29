package com.devgomes.glpi_integration.web;

import com.devgomes.glpi_integration.dto.IdNameItem;
import com.devgomes.glpi_integration.service.GlpiIntegrationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Consultas auxiliares (lookup)", description = "Ids para montar planilhas (usuários, status, localidades, …)")
@RestController
public class LookupSummaryController {

    private final GlpiIntegrationService glpiIntegrationService;

    public LookupSummaryController(GlpiIntegrationService glpiIntegrationService) {
        this.glpiIntegrationService = glpiIntegrationService;
    }

    @GetMapping("/api/users/summary")
    public ResponseEntity<List<IdNameItem>> listUsers(@RequestParam(defaultValue = "0-999") String range) {
        return ResponseEntity.ok(glpiIntegrationService.listUserIdAndLogins(range));
    }

    @GetMapping("/api/states/summary")
    public ResponseEntity<List<IdNameItem>> listStates(@RequestParam(defaultValue = "0-999") String range) {
        return ResponseEntity.ok(glpiIntegrationService.listStateIdAndNames(range));
    }

    @GetMapping("/api/locations/summary")
    public ResponseEntity<List<IdNameItem>> listLocations(@RequestParam(defaultValue = "0-999") String range) {
        return ResponseEntity.ok(glpiIntegrationService.listLocationIdAndNames(range));
    }

    @GetMapping("/api/groups/summary")
    public ResponseEntity<List<IdNameItem>> listGroups(@RequestParam(defaultValue = "0-999") String range) {
        return ResponseEntity.ok(glpiIntegrationService.listGroupIdAndNames(range));
    }

    @GetMapping("/api/computer-types/summary")
    public ResponseEntity<List<IdNameItem>> listComputerTypes(@RequestParam(defaultValue = "0-999") String range) {
        return ResponseEntity.ok(glpiIntegrationService.listComputerTypeIdAndNames(range));
    }

    @GetMapping("/api/manufacturers/summary")
    public ResponseEntity<List<IdNameItem>> listManufacturers(@RequestParam(defaultValue = "0-999") String range) {
        return ResponseEntity.ok(glpiIntegrationService.listManufacturerIdAndNames(range));
    }
}
