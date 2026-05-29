package com.devgomes.glpi_integration.web;

import com.devgomes.glpi_integration.sync.ComputerSyncBatchService;
import com.devgomes.glpi_integration.sync.GenericItemSyncBatchService;
import com.devgomes.glpi_integration.sync.SyncReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

@Tag(name = "Sincronização (planilha)", description = "Upload CSV/XLSX. Campo obrigatório: file. Use /validate antes do sync.")
@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final ComputerSyncBatchService computerBatchService;
    private final GenericItemSyncBatchService customBatchService;

    public SyncController(
            ComputerSyncBatchService computerBatchService,
            GenericItemSyncBatchService customBatchService) {
        this.computerBatchService = computerBatchService;
        this.customBatchService = customBatchService;
    }

    @PostMapping(value = "/computers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SyncReport> syncComputers(@RequestParam("file") MultipartFile file) throws Exception {
        return ResponseEntity.ok(runWithTempFile(file, computerBatchService::syncFromFile));
    }

    @PostMapping(value = "/computers/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SyncReport> validateComputers(@RequestParam("file") MultipartFile file) throws Exception {
        return ResponseEntity.ok(runWithTempFile(file, computerBatchService::validateFromFile));
    }

    @PostMapping("/computers/run")
    public ResponseEntity<SyncReport> syncFromConfiguredPath() throws Exception {
        return ResponseEntity.ok(computerBatchService.syncFromConfiguredPath());
    }

    @Operation(summary = "Sincronizar Starlink", description = "Cria ou atualiza itens no GLPI. Chave natural: nome.")
    @PostMapping(value = "/starlink", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SyncReport> syncStarlink(
            @Parameter(description = "Planilha CSV ou XLSX", required = true)
            @RequestParam("file") MultipartFile file) throws Exception {
        return ResponseEntity.ok(runWithTempFile(file, path -> customBatchService.syncFromFile("starlink", path)));
    }

    @Operation(summary = "Validar Starlink (dry-run)", description = "Não grava no GLPI; retorna would_create / would_update.")
    @PostMapping(value = "/starlink/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SyncReport> validateStarlink(
            @Parameter(description = "Planilha CSV ou XLSX", required = true)
            @RequestParam("file") MultipartFile file) throws Exception {
        return ResponseEntity.ok(runWithTempFile(file, path -> customBatchService.validateFromFile("starlink", path)));
    }

    @PostMapping(value = "/chip", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SyncReport> syncChip(@RequestParam("file") MultipartFile file) throws Exception {
        return ResponseEntity.ok(runWithTempFile(file, path -> customBatchService.syncFromFile("chip", path)));
    }

    @PostMapping(value = "/chip/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SyncReport> validateChip(@RequestParam("file") MultipartFile file) throws Exception {
        return ResponseEntity.ok(runWithTempFile(file, path -> customBatchService.validateFromFile("chip", path)));
    }

    @PostMapping(value = "/celular", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SyncReport> syncCelular(@RequestParam("file") MultipartFile file) throws Exception {
        return ResponseEntity.ok(runWithTempFile(file, path -> customBatchService.syncFromFile("celular", path)));
    }

    @PostMapping(value = "/celular/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SyncReport> validateCelular(@RequestParam("file") MultipartFile file) throws Exception {
        return ResponseEntity.ok(runWithTempFile(file, path -> customBatchService.validateFromFile("celular", path)));
    }

    @FunctionalInterface
    private interface PathSyncOperation {
        SyncReport apply(Path path) throws Exception;
    }

    private SyncReport runWithTempFile(MultipartFile file, PathSyncOperation operation) throws Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo vazio");
        }
        Path temp = Files.createTempFile("glpi-sync-", "-" + file.getOriginalFilename());
        try {
            file.transferTo(temp);
            return operation.apply(temp);
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
