package com.devgomes.glpi_integration.sync;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class AssetSpreadsheetReader {

    private static final DataFormatter FORMATTER = new DataFormatter();

    public List<AssetUpdateRow> read(Path file) throws IOException {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".csv")) {
            return readCsv(file);
        }
        if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
            return readExcel(file);
        }
        throw new IllegalArgumentException("Formato não suportado: " + name + " (use .csv, .xls ou .xlsx)");
    }

    private List<AssetUpdateRow> readCsv(Path file) throws IOException {
        List<String[]> rows = CsvFormatSupport.readRows(file);
        return parseRows(file.toString(), rows);
    }

    private List<AssetUpdateRow> readExcel(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file);
             Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<String[]> rows = new ArrayList<>();
            for (Row row : sheet) {
                if (row == null) {
                    continue;
                }
                int lastCell = row.getLastCellNum();
                if (lastCell < 0) {
                    continue;
                }
                String[] values = new String[lastCell];
                for (int i = 0; i < lastCell; i++) {
                    Cell cell = row.getCell(i);
                    values[i] = cell == null ? "" : FORMATTER.formatCellValue(cell).trim();
                }
                rows.add(values);
            }
            return parseRows(file.toString(), rows);
        }
    }

    private List<AssetUpdateRow> parseRows(String source, List<String[]> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<String, Integer> headerIndex = buildHeaderIndex(rows.getFirst());
        List<AssetUpdateRow> result = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (isBlankRow(row)) {
                continue;
            }
            int lineNumber = i + 1;
            try {
                result.add(mapRow(lineNumber, row, headerIndex));
            } catch (Exception ex) {
                throw new IllegalArgumentException(
                        "Erro na linha " + lineNumber + " de " + source + ": " + ex.getMessage(), ex);
            }
        }
        return result;
    }

    private Map<String, Integer> buildHeaderIndex(String[] header) {
        Map<String, Integer> index = new HashMap<>();
        List<String> detected = new ArrayList<>();
        for (int i = 0; i < header.length; i++) {
            String raw = CsvFormatSupport.sanitizeHeaderCell(header[i]);
            detected.add(raw);
            String normalized = normalizeHeader(raw);
            index.put(normalized, i);
            registerAliases(index, normalized, i);
        }

        boolean hasGlpiId = index.containsKey("glpi_id");
        boolean hasAssetId = index.containsKey("id_ativo");
        if (!hasGlpiId && !hasAssetId) {
            throw new IllegalArgumentException(
                    "Coluna obrigatória ausente: informe id_ativo (ID do Computer no GLPI). "
                            + "Colunas detectadas: " + detected);
        }
        return index;
    }

    private static void registerAliases(Map<String, Integer> index, String normalized, int column) {
        if (isSerialColumn(normalized)) {
            putAlias(index, "serial", column);
        }
        if (isAssetIdColumn(normalized)) {
            putAlias(index, "id_ativo", column);
        }
        if (normalized.contains("id_model") || normalized.equals("modelo")
                || normalized.equals("computermodels_id")) {
            putAlias(index, "computermodels_id", column);
        }
        if (normalized.equals("responsavel") || normalized.equals("users_id")) {
            putAlias(index, "users_id", column);
        }
        if (normalized.equals("status") || normalized.equals("states_id")) {
            putAlias(index, "states_id", column);
        }
        if (normalized.equals("glpi_id")) {
            putAlias(index, "glpi_id", column);
        }
    }

    private static boolean isAssetIdColumn(String normalized) {
        if (normalized.isBlank()) {
            return false;
        }
        if (normalized.contains("id_model") || normalized.contains("model")) {
            return false;
        }
        return normalized.startsWith("id_ativo")
                || normalized.equals("id_do_ativo")
                || normalized.equals("id")
                || normalized.equals("name")
                || (normalized.contains("ativo") && normalized.contains("id"));
    }

    private static void putAlias(Map<String, Integer> index, String key, int column) {
        index.putIfAbsent(key, column);
    }

    private AssetUpdateRow mapRow(int lineNumber, String[] row, Map<String, Integer> headerIndex) {
        Integer glpiIdColumn = parseOptionalInt(row, headerIndex, "glpi_id");
        String idAtivoRaw = parseOptionalString(row, headerIndex, "id_ativo");

        int resolvedGlpiId = 0;
        String lookupName = null;

        if (glpiIdColumn != null && glpiIdColumn > 0) {
            resolvedGlpiId = glpiIdColumn;
        } else if (idAtivoRaw != null) {
            if (SyncFieldResolver.isNumeric(idAtivoRaw)) {
                resolvedGlpiId = SyncFieldResolver.parsePositiveInt(idAtivoRaw);
            } else {
                lookupName = idAtivoRaw.trim();
            }
        } else {
            throw new IllegalArgumentException("Informe glpi_id ou id_ativo na linha " + lineNumber);
        }

        var responsavel = parseResponsavel(row, headerIndex);
        var status = parseStatus(row, headerIndex);

        return new AssetUpdateRow(
                lineNumber,
                resolvedGlpiId,
                lookupName,
                responsavel.usersId(),
                responsavel.login(),
                status.statesId(),
                status.label(),
                parseOptionalInt(row, headerIndex, "computermodels_id"),
                parseOptionalString(row, headerIndex, "serial"),
                parseOptionalString(row, headerIndex, "otherserial"),
                parseOptionalString(row, headerIndex, "comment")
        );
    }

    private static ResponsavelFields parseResponsavel(String[] row, Map<String, Integer> headerIndex) {
        String raw = parseOptionalString(row, headerIndex, "users_id");
        if (raw == null) {
            return new ResponsavelFields(null, null);
        }
        if (SyncFieldResolver.isNumeric(raw)) {
            return new ResponsavelFields(Integer.parseInt(raw.trim()), null);
        }
        return new ResponsavelFields(null, raw.trim());
    }

    private static StatusFields parseStatus(String[] row, Map<String, Integer> headerIndex) {
        String raw = parseOptionalString(row, headerIndex, "states_id");
        if (raw == null) {
            return new StatusFields(null, null);
        }
        if (SyncFieldResolver.isNumeric(raw)) {
            return new StatusFields(Integer.parseInt(raw.trim()), null);
        }
        return new StatusFields(null, raw.trim());
    }

    private record ResponsavelFields(Integer usersId, String login) {
    }

    private record StatusFields(Integer statesId, String label) {
    }

    private static boolean isSerialColumn(String normalized) {
        return normalized.equals("service_tag")
                || normalized.startsWith("service_tag")
                || (normalized.contains("service") && normalized.contains("serial"));
    }

    static String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
        return CsvFormatSupport.sanitizeHeaderCell(header)
                .toLowerCase(Locale.ROOT)
                .replace('(', ' ')
                .replace(')', ' ')
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    private static boolean isBlankRow(String[] row) {
        for (String cell : row) {
            if (cell != null && !cell.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static Integer parseOptionalInt(String[] row, Map<String, Integer> headerIndex, String column) {
        String raw = cell(row, headerIndex, column);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Integer.parseInt(raw.trim());
    }

    private static String parseOptionalString(String[] row, Map<String, Integer> headerIndex, String column) {
        String raw = cell(row, headerIndex, column);
        if (SyncFieldResolver.isNullLiteral(raw)) {
            return null;
        }
        return raw.trim();
    }

    private static String cell(String[] row, Map<String, Integer> headerIndex, String column) {
        Integer idx = headerIndex.get(column);
        if (idx == null || idx >= row.length) {
            return null;
        }
        return row[idx];
    }
}
