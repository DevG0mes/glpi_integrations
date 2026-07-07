package com.devgomes.glpi_integration.sync;

import com.devgomes.glpi_integration.client.GlpiApiException;
import com.devgomes.glpi_integration.config.GlpiSyncProperties;
import com.devgomes.glpi_integration.dto.ComputerUpdateRequest;
import com.devgomes.glpi_integration.service.GlpiIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        return processRows(file.toString(), rows, false);
    }

    public SyncReport validateFromFile(Path file) throws IOException {
        List<AssetUpdateRow> rows = spreadsheetReader.read(file);
        return processRows(file.toString(), rows, true);
    }

    public SyncReport syncFromConfiguredPath() throws IOException {
        if (!syncProperties.hasInputPath()) {
            throw new IllegalStateException("glpi.sync.input-path não configurado");
        }
        return syncFromFile(Path.of(syncProperties.getInputPath()));
    }

    public SyncReport processRows(String source, List<AssetUpdateRow> rows) {
        return processRows(source, rows, false);
    }

    public SyncReport processRows(String source, List<AssetUpdateRow> rows, boolean dryRun) {
        log.info("{} {} linhas a partir de {}",
                dryRun ? "Validando" : "Iniciando sincronização de",
                rows.size(),
                source);
        glpiIntegrationService.initSession();

        String range = syncProperties.getLookupRange();
        SyncLookupIndexes indexes = glpiIntegrationService.buildSyncLookupIndexes(range);
        Set<Integer> computerIds = glpiIntegrationService.buildComputerIdSet(range);
        Map<String, Integer> computersByName = glpiIntegrationService.buildComputerNameIndex(range);

        List<SyncLineResult> results = new ArrayList<>();
        int success = 0;
        int failure = 0;

        for (AssetUpdateRow row : rows) {
            try {
                ComputerUpdateRequest request = SyncFieldResolver.toUpdateRequest(row, indexes);
                ComputerMutationPlan plan = planMutation(row, request, computerIds, computersByName);
                if (plan.request().toInputMap().isEmpty()) {
                    throw new IllegalArgumentException(
                            "Linha " + row.lineNumber() + ": nenhum campo para sincronizar");
                }
                if (dryRun) {
                    results.add(new SyncLineResult(row.lineNumber(), plan.targetId(), true,
                            plan.actionLabel() + ": " + plan.request().toInputMap()));
                } else {
                    int resultingId = executeMutation(plan);
                    if (plan.isCreate()) {
                        computerIds.add(resultingId);
                        addNameIndex(plan.request(), resultingId, computersByName);
                    }
                    results.add(new SyncLineResult(row.lineNumber(), resultingId, true,
                            plan.isCreate() ? "OK (criado)" : "OK (atualizado)"));
                }
                success++;
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

        log.info("{} concluída: {} sucesso, {} falha(s), total {}",
                dryRun ? "Validação" : "Sincronização", success, failure, rows.size());
        return new SyncReport(source, rows.size(), success, failure, results);
    }

    private ComputerMutationPlan planMutation(
            AssetUpdateRow row,
            ComputerUpdateRequest request,
            Set<Integer> computerIds,
            Map<String, Integer> computersByName) {
        if (row.glpiId() > 0) {
            if (computerIds.contains(row.glpiId())) {
                return new ComputerMutationPlan(false, row.glpiId(), request);
            }
            return new ComputerMutationPlan(true, row.glpiId(), ensureCreateName(row, request));
        }
        if (row.assetName() == null || row.assetName().isBlank()) {
            throw new IllegalArgumentException("Linha " + row.lineNumber() + ": informe glpi_id ou id_ativo");
        }
        String key = GlpiIntegrationService.normalizeNameKey(row.assetName());
        Integer id = computersByName.get(key);
        if (id != null) {
            return new ComputerMutationPlan(false, id, request);
        }
        return new ComputerMutationPlan(true, 0, ensureCreateName(row, request));
    }

    private ComputerUpdateRequest ensureCreateName(AssetUpdateRow row, ComputerUpdateRequest request) {
        if (request.name() != null && !request.name().isBlank()) {
            return request;
        }
        if (row.assetName() != null && !row.assetName().isBlank()) {
            return new ComputerUpdateRequest(
                    request.usersId(),
                    request.groupsId(),
                    request.locationsId(),
                    request.statesId(),
                    request.computermodelsId(),
                    request.computertypesId(),
                    request.manufacturersId(),
                    row.assetName(),
                    request.serial(),
                    request.otherserial(),
                    request.comment(),
                    request.vencimentoGarantia(),
                    request.codMega()
            );
        }
        return request;
    }

    private int executeMutation(ComputerMutationPlan plan) {
        if (plan.isCreate()) {
            return glpiIntegrationService.createComputer(plan.request());
        }
        glpiIntegrationService.updateComputer(plan.targetId(), plan.request());
        return plan.targetId();
    }

    private void addNameIndex(ComputerUpdateRequest request, int computerId, Map<String, Integer> computersByName) {
        if (request.name() == null || request.name().isBlank()) {
            return;
        }
        computersByName.put(GlpiIntegrationService.normalizeNameKey(request.name()), computerId);
    }

    private record ComputerMutationPlan(boolean isCreate, int targetId, ComputerUpdateRequest request) {
        String actionLabel() {
            return isCreate ? "would_create" : "would_update";
        }
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
