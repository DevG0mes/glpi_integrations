package com.devgomes.glpi_integration.templates;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Gera planilhas modelo em {@code docs/templates/}. Rode quando precisar atualizar:
 * {@code mvn -Dtest=SpreadsheetTemplatesGeneratorTest test}
 */
class SpreadsheetTemplatesGeneratorTest {

    private static final Path OUTPUT_DIR = Path.of("docs", "templates");

    @Test
    void geraPlanilhaUnificadaComTresAbas() throws IOException {
        Files.createDirectories(OUTPUT_DIR);
        Path output = OUTPUT_DIR.resolve("templates_sincronizacao_ativos.xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {
            criarAbaInstrucoes(workbook);
            criarAbaStarlink(workbook);
            criarAbaChip(workbook);
            criarAbaCelular(workbook);
            try (var out = Files.newOutputStream(output)) {
                workbook.write(out);
            }
        }
    }

    @Test
    void geraPlanilhasIndividuais() throws IOException {
        Files.createDirectories(OUTPUT_DIR);
        try (Workbook starlink = new XSSFWorkbook()) {
            criarAbaStarlink(starlink);
            escrever(starlink, OUTPUT_DIR.resolve("template_starlink.xlsx"));
        }
        try (Workbook chip = new XSSFWorkbook()) {
            criarAbaChip(chip);
            escrever(chip, OUTPUT_DIR.resolve("template_chip.xlsx"));
        }
        try (Workbook celular = new XSSFWorkbook()) {
            criarAbaCelular(celular);
            escrever(celular, OUTPUT_DIR.resolve("template_celular.xlsx"));
        }
    }

    private static void escrever(Workbook workbook, Path path) throws IOException {
        try (var out = Files.newOutputStream(path)) {
            workbook.write(out);
        }
    }

    private static void criarAbaInstrucoes(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Instrucoes");
        String[] lines = {
                "Templates de sincronizacao — GLPI Integration",
                "",
                "Starlink: POST /api/sync/starlink/validate  |  POST /api/sync/starlink",
                "Chip:     POST /api/sync/chip/validate      |  POST /api/sync/chip",
                "Celular:  POST /api/sync/celular/validate   |  POST /api/sync/celular",
                "",
                "Resumos: GET /api/users/summary, /api/states/summary, /api/locations/summary",
                "         GET /api/custom-assets/{starlink|chip|celular}/summary",
                "",
                "Apague a linha de exemplo (linha 3) antes de importar dados reais.",
                "Nao versionar senhas reais."
        };
        for (int i = 0; i < lines.length; i++) {
            sheet.createRow(i).createCell(0).setCellValue(lines[i]);
        }
        sheet.setColumnWidth(0, 22_000);
    }

    private static void criarAbaStarlink(Workbook workbook) {
        String[] headers = {
                "id_ativo", "nome", "projeto", "responsavel", "email",
                "senha_conta", "senha_roteador", "localidade"
        };
        Object[] exemplo = {
                1, "STARLINK-Projeto-Exemplo-SP", "Projeto Exemplo", "usuario.glpi",
                "contato@empresa.com.br", "", "", "Sao Paulo"
        };
        criarAbaDados(workbook, "Starlink", headers, exemplo);
    }

    private static void criarAbaChip(Workbook workbook) {
        String[] headers = {"id_ativo", "iccid", "numero", "responsavel", "status"};
        Object[] exemplo = {1, "8955012345678901234", "11999990000", "usuario.glpi", "Em uso"};
        criarAbaDados(workbook, "Chip", headers, exemplo);
    }

    private static void criarAbaCelular(Workbook workbook) {
        String[] headers = {"id_ativo", "imei", "modelo", "responsavel", "nome"};
        Object[] exemplo = {1, "351234567890123", "Modelo Exemplo", "usuario.glpi", "Celular Joao"};
        criarAbaDados(workbook, "Celular", headers, exemplo);
    }

    private static void criarAbaDados(
            Workbook workbook,
            String nomeAba,
            String[] headers,
            Object[] exemplo) {
        Sheet sheet = workbook.createSheet(nomeAba);
        CellStyle headerStyle = estiloCabecalho(workbook);

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, Math.max(4_000, headers[i].length() * 400));
        }

        Row dataRow = sheet.createRow(1);
        for (int i = 0; i < exemplo.length; i++) {
            setCellValue(dataRow.createCell(i), exemplo[i]);
        }

        sheet.createFreezePane(0, 2);
    }

    private static CellStyle estiloCabecalho(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private static void setCellValue(Cell cell, Object value) {
        switch (value) {
            case null -> cell.setBlank();
            case Number number -> cell.setCellValue(number.doubleValue());
            case Boolean bool -> cell.setCellValue(bool);
            default -> cell.setCellValue(value.toString());
        }
    }
}
