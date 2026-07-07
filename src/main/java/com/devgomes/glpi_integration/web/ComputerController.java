package com.devgomes.glpi_integration.web;

import com.devgomes.glpi_integration.dto.ComputerListResponse;
import com.devgomes.glpi_integration.dto.ComputerUpdateRequest;
import com.devgomes.glpi_integration.dto.IdNameItem;
import com.devgomes.glpi_integration.service.ComputerWarrantyReportService;
import com.devgomes.glpi_integration.service.GlpiIntegrationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Computer", description = "Equipamento nativo GLPI")
@RestController
@RequestMapping("/api/computers")
public class ComputerController {

    private final GlpiIntegrationService glpiIntegrationService;
    private final ComputerWarrantyReportService computerWarrantyReportService;

    public ComputerController(
            GlpiIntegrationService glpiIntegrationService,
            ComputerWarrantyReportService computerWarrantyReportService) {
        this.glpiIntegrationService = glpiIntegrationService;
        this.computerWarrantyReportService = computerWarrantyReportService;
    }

    /** Somente id + name de cada Computer. */
    @GetMapping("/summary")
    public ResponseEntity<List<IdNameItem>> listIdAndName(
            @RequestParam(defaultValue = "0-999") String range) {
        return ResponseEntity.ok(glpiIntegrationService.listComputerIdAndNames(range));
    }

    /** Lista Computers do GLPI (campos completos). Paginação: {@code ?range=0-999} */
    @GetMapping
    public ResponseEntity<ComputerListResponse> listComputers(
            @RequestParam(defaultValue = "0-999") String range,
            @RequestParam(defaultValue = "false") boolean expandDropdowns) {
        return ResponseEntity.ok(glpiIntegrationService.listComputers(range, expandDropdowns));
    }

    @GetMapping("/report")
    public ResponseEntity<Map<String, Object>> computerReport(
            @RequestParam(defaultValue = "0-999") String range) {
        return ResponseEntity.ok(computerWarrantyReportService.buildReport(range));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getComputer(
            @PathVariable("id") int computerId,
            @RequestParam(defaultValue = "false") boolean expandDropdowns) {
        return ResponseEntity.ok(glpiIntegrationService.getComputer(computerId, expandDropdowns));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateComputer(
            @PathVariable("id") int computerId,
            @RequestBody ComputerUpdateRequest request) {
        glpiIntegrationService.updateComputer(computerId, request);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("computerId", computerId);
        body.put("status", "updated");
        body.put("fieldsSent", request.toInputMap());
        return ResponseEntity.ok(body);
    }
}
