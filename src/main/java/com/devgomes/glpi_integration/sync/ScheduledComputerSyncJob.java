package com.devgomes.glpi_integration.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "glpi.sync", name = "enabled", havingValue = "true")
public class ScheduledComputerSyncJob {

    private static final Logger log = LoggerFactory.getLogger(ScheduledComputerSyncJob.class);

    private final ComputerSyncBatchService batchService;

    public ScheduledComputerSyncJob(ComputerSyncBatchService batchService) {
        this.batchService = batchService;
    }

    @Scheduled(fixedDelayString = "${glpi.sync.schedule-delay-ms:3600000}")
    public void runScheduledSync() {
        try {
            SyncReport report = batchService.syncFromConfiguredPath();
            log.info("Job agendado finalizado: {} sucesso / {} falhas", report.successCount(), report.failureCount());
        } catch (Exception ex) {
            log.error("Job agendado de sincronização falhou: {}", ex.getMessage(), ex);
        }
    }
}
