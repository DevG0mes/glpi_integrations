package com.devgomes.glpi_integration.sync;

import com.devgomes.glpi_integration.config.GlpiCustomAssetsProperties;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class CustomAssetSpreadsheetReader {

    private static final DataFormatter FORMATTER = new DataFormatter();

    private final AssetTypeRegistry assetTypeRegistry;

    public CustomAssetSpreadsheetReader(AssetTypeRegistry assetTypeRegistry) {
        this.assetTypeRegistry = assetTypeRegistry;
    }

    public List<CustomAssetRow> read(Path file, String assetKey) throws IOException {
        GlpiCustomAssetsProperties.CustomAssetDefinition definition = assetTypeRegistry.get(assetKey);
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        List<String[]> rows;
        if (name.endsWith(".csv")) {
            rows = CsvFormatSupport.readRows(file);
        } else if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
            rows = readExcel(file);
        } else {
            throw new IllegalArgumentException("Formato não suportado: " + name);
        }
        return parseRows(file.toString(), rows, definition);
    }

    private List<String[]> readExcel(Path file) throws IOException {
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
            return rows;
        }
    }

    private List<CustomAssetRow> parseRows(
            String source,
            List<String[]> rows,
            GlpiCustomAssetsProperties.CustomAssetDefinition definition) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<String, Integer> headerIndex = buildHeaderIndex(rows.getFirst(), definition);
        List<CustomAssetRow> result = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (isBlankRow(row) || isInstructionRow(row)) {
                continue;
            }
            int lineNumber = i + 1;
            try {
                result.add(mapRow(lineNumber, row, headerIndex, definition));
            } catch (Exception ex) {
                throw new IllegalArgumentException(
                        "Erro na linha " + lineNumber + " de " + source + ": " + ex.getMessage(), ex);
            }
        }
        return result;
    }

    private Map<String, Integer> buildHeaderIndex(
            String[] header,
            GlpiCustomAssetsProperties.CustomAssetDefinition definition) {
        Map<String, Integer> index = new HashMap<>();
        List<String> detected = new ArrayList<>();
        boolean hasIdAtivo = false;
        boolean hasNaturalKey = false;

        for (int i = 0; i < header.length; i++) {
            String raw = CsvFormatSupport.sanitizeHeaderCell(header[i]);
            detected.add(raw);
            String normalized = AssetSpreadsheetReader.normalizeHeader(raw);
            index.put(normalized, i);
            for (String alias : definition.columns().keySet()) {
                if (normalized.equals(alias) || normalized.contains(alias)) {
                    index.putIfAbsent(alias, i);
                }
            }
            if (normalized.equals("id_ativo") || normalized.equals("glpi_id") || normalized.equals("id")) {
                hasIdAtivo = true;
                index.putIfAbsent("id_ativo", i);
            }
            for (String alias : CustomAssetNaturalKeySupport.spreadsheetAliases(definition)) {
                if (registerNaturalKeyColumn(index, normalized, i, alias)) {
                    hasNaturalKey = true;
                }
            }
        }

        if (!hasIdAtivo && !hasNaturalKey) {
            throw new IllegalArgumentException(
                    "Coluna obrigatória ausente: "
                            + CustomAssetNaturalKeySupport.requiredColumnsHint(definition)
                            + ". Colunas detectadas: " + detected);
        }
        return index;
    }

    private CustomAssetRow mapRow(
            int lineNumber,
            String[] row,
            Map<String, Integer> headerIndex,
            GlpiCustomAssetsProperties.CustomAssetDefinition definition) {
        int glpiId = 0;
        String idRaw = cell(row, headerIndex, "id_ativo");
        if (idRaw == null) {
            idRaw = cell(row, headerIndex, "glpi_id");
        }
        if (idRaw != null && SyncFieldResolver.isNumeric(idRaw)) {
            glpiId = SyncFieldResolver.parsePositiveInt(idRaw);
        }

        Map<String, String> values = new LinkedHashMap<>();
        for (String alias : definition.columns().keySet()) {
            String raw = cell(row, headerIndex, alias);
            if (!SyncFieldResolver.isNullLiteral(raw)) {
                values.put(alias, raw.trim());
            }
        }

        String naturalKey = CustomAssetNaturalKeySupport.resolveFromRow(row, headerIndex, definition, values);
        if (glpiId <= 0 && (naturalKey == null || naturalKey.isBlank())) {
            throw new IllegalArgumentException(
                    CustomAssetNaturalKeySupport.rowMissingKeyMessage(lineNumber, definition));
        }

        return new CustomAssetRow(lineNumber, glpiId, naturalKey, values);
    }

    private static boolean registerNaturalKeyColumn(
            Map<String, Integer> index,
            String normalizedHeader,
            int columnIndex,
            String keyAlias) {
        if (normalizedHeader.equals(keyAlias)) {
            index.putIfAbsent(keyAlias, columnIndex);
            return true;
        }
        return false;
    }

    private static String cell(String[] row, Map<String, Integer> headerIndex, String column) {
        Integer idx = headerIndex.get(column);
        if (idx == null || idx >= row.length) {
            return null;
        }
        return row[idx];
    }

    private static boolean isBlankRow(String[] row) {
        for (String cell : row) {
            if (cell != null && !cell.isBlank()) {
                return false;
            }
        }
        return true;
    }

    /** Ignora linha de instruções de templates antigos (texto longo na primeira coluna). */
    private static boolean isInstructionRow(String[] row) {
        if (row.length == 0 || row[0] == null) {
            return false;
        }
        String first = row[0].trim().toLowerCase(Locale.ROOT);
        return first.startsWith("obrigatorio")
                || first.startsWith("obrigatório")
                || first.startsWith("obrigatoria")
                || first.contains("login glpi");
    }
}
