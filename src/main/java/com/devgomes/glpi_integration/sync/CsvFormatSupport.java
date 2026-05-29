package com.devgomes.glpi_integration.sync;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Detecta encoding e separador de CSV exportado pelo Excel (vírgula ou ponto-e-vírgula). */
final class CsvFormatSupport {

    private static final Charset[] CHARSET_CANDIDATES = {
            StandardCharsets.UTF_8,
            Charset.forName("Windows-1252"),
            StandardCharsets.ISO_8859_1
    };

    private CsvFormatSupport() {
    }

    static List<String[]> readRows(Path file) throws IOException {
        IOException lastFailure = null;
        for (Charset charset : CHARSET_CANDIDATES) {
            try {
                String content = stripBom(Files.readString(file, charset));
                if (content.isBlank()) {
                    return List.of();
                }
                char separator = detectSeparator(firstLine(content));
                return parseWithSeparator(content, separator);
            } catch (IOException ex) {
                lastFailure = ex;
            } catch (RuntimeException ex) {
                lastFailure = new IOException(ex);
            }
        }
        if (lastFailure != null) {
            throw lastFailure;
        }
        throw new IOException("Não foi possível ler o CSV: " + file);
    }

    static char detectSeparator(String firstLine) {
        int commas = countUnquoted(firstLine, ',');
        int semicolons = countUnquoted(firstLine, ';');
        int tabs = countUnquoted(firstLine, '\t');
        if (semicolons > commas && semicolons >= tabs && semicolons > 0) {
            return ';';
        }
        if (tabs > commas && tabs > semicolons && tabs > 0) {
            return '\t';
        }
        return ',';
    }

    static String stripBom(String value) {
        if (value == null || value.isEmpty()) {
            return value == null ? "" : value;
        }
        if (value.charAt(0) == '\ufeff') {
            return value.substring(1);
        }
        return value;
    }

    static String sanitizeHeaderCell(String header) {
        if (header == null) {
            return "";
        }
        return stripBom(header)
                .replace("\u200b", "")
                .replace("\u00a0", " ")
                .trim();
    }

    private static String firstLine(String content) {
        int newline = content.indexOf('\n');
        if (newline < 0) {
            return content.replace("\r", "");
        }
        return content.substring(0, newline).replace("\r", "");
    }

    private static List<String[]> parseWithSeparator(String content, char separator) {
        try (CSVReader reader = new CSVReaderBuilder(new StringReader(content))
                .withCSVParser(new CSVParserBuilder().withSeparator(separator).build())
                .build()) {
            return reader.readAll();
        } catch (CsvException | IOException ex) {
            throw new IllegalArgumentException("CSV inválido: " + ex.getMessage(), ex);
        }
    }

    private static int countUnquoted(String line, char target) {
        boolean inQuotes = false;
        int count = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (!inQuotes && c == target) {
                count++;
            }
        }
        return count;
    }
}
