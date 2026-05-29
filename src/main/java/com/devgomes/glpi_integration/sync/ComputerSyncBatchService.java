package com.devgomes.glpi_integration.sync;

import com.devgomes.glpi_integration.client.GlpiApiException;
import com.devgomes.glpi_integration.config.GlpiSyncProperties;
import com.devgomes.glpi_integration.service.GlpiIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ComputerSyncBatchService {

    private static final Logger log = LoggerFactory.getLogger(ComputerSyncBatchService.class);

    private final AssetSpreadsheetReader spreadsheetReader;
    private final GlpiIntegrationService glpiIntegrationService;
    private final GlpiSyncProperties syncProperties;

    public ComputerSyncBatchService(
            AssetSpreadsheetReader spreadsheetReader,
            GlpiIntegrationService glpiIntegrationService,
            GlpiSyncProperties syncProperties) {
        this.spreadsheetReader = spreadsheetReader;
        this.glpiIntegrationService = glpiIntegrationService;
        this.syncProperties = syncProperties;
    }

    public SyncReport syncFromFile(Path file) throws IOException {
        List<AssetUpdateRow> rows = spreadsheetReader.read(file);
        return processRows(file.toString(), rows);
    }

    public SyncReport syncFromConfiguredPath() throws IOException {
        if (!syncProperties.hasInputPath()) {
            throw new IllegalStateException("glpi.sync.input-path não configurado");
        }
        return syncFromFile(Path.of(syncProperties.getInputPath()));
    }

    public SyncReport processRows(String source, List<AssetUpdateRow> rows) {
        log.info("Iniciando sincronização de {} linhas a partir de {}", rows.size(), source);
        glpiIntegrationService.initSession();

        String range = syncProperties.getLookupRange();
        Map<String, Integer> computersByName = glpiIntegrationService.buildComputerNameIndex(range);
        Map<String, Integer> usersByLogin = glpiIntegrationService.buildUserLoginIndex(range);
        Map<String, Integer> statesByLabel = glpiIntegrationService.buildStateLabelIndex(range);

        List<SyncLineResult> results = new ArrayList<>();
        int success = 0;
        int failure = 0;

        for (AssetUpdateRow row : rows) {
            try {
                int computerId = resolveComputerId(row, computersByName);
                var request = SyncFieldResolver.toUpdateRequest(row, usersByLogin, statesByLabel);
                if (request.toInputMap().isEmpty()) {
                    throw new IllegalArgumentException(
                            "Linha " + row.lineNumber() + ": nenhum campo para atualizar (informe id_model)");
                }
                glpiIntegrationService.updateComputer(computerId, request);
                results.add(new SyncLineResult(row.lineNumber(), computerId, true, "OK"));
                success++;
                log.debug("Linha {}: Computer {} atualizado", row.lineNumber(), computerId);
            } catch (GlpiApiException ex) {
                String message = "GLPI " + ex.getStatusCode().value() + ": " + ex.getMessage();
                results.add(new SyncLineResult(row.lineNumber(), row.glpiId(), false, message));
                failure++;
                log.warn("Linha {}: falha — {}", row.lineNumber(), message);
            } catch (Exception ex) {
                results.add(new SyncLineResult(row.lineNumber(), row.glpiId(), false, ex.getMessage()));
                failure++;
                log.warn("Linha {}: falha — {}", row.lineNumber(), ex.getMessage());
            }
            throttle();
        }

        log.info("Sincronização concluída: {} sucesso, {} falha(s), total {}", success, failure, rows.size());
        return new SyncReport(source, rows.size(), success, failure, results);
    }

    private int resolveComputerId(AssetUpdateRow row, Map<String, Integer> computersByName) {
        if (row.glpiId() > 0) {
            return row.glpiId();
        }
        if (row.assetName() == null || row.assetName().isBlank()) {
            throw new IllegalArgumentException("Linha " + row.lineNumber() + ": informe glpi_id ou id_ativo");
        }
        String key = GlpiIntegrationService.normalizeNameKey(row.assetName());
        Integer id = computersByName.get(key);
        if (id == null) {
            throw new IllegalArgumentException(
                    "Linha " + row.lineNumber() + ": Computer não encontrado no GLPI com name='" + row.assetName() + "'");
        }
        return id;
    }

    private void throttle() {
        long delay = syncProperties.getDelayMs();
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
