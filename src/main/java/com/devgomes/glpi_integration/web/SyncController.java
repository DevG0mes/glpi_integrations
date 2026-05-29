package com.devgomes.glpi_integration.web;

import com.devgomes.glpi_integration.sync.ComputerSyncBatchService;
import com.devgomes.glpi_integration.sync.SyncReport;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final ComputerSyncBatchService batchService;

    public SyncController(ComputerSyncBatchService batchService) {
        this.batchService = batchService;
    }

    @PostMapping(value = "/computers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SyncReport> syncComputers(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Path temp = Files.createTempFile("glpi-sync-", "-" + file.getOriginalFilename());
        try {
            file.transferTo(temp);
            SyncReport report = batchService.syncFromFile(temp);
            return ResponseEntity.ok(report);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    @PostMapping("/computers/run")
    public ResponseEntity<SyncReport> syncFromConfiguredPath() throws Exception {
        return ResponseEntity.ok(batchService.syncFromConfiguredPath());
    }
}
