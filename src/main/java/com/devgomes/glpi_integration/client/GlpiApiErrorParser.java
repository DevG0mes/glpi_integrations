package com.devgomes.glpi_integration.client;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Interpreta respostas de erro da API legada GLPI ({@code ["ERROR_GLPI_UPDATE", "..."]}).
 */
public final class GlpiApiErrorParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private GlpiApiErrorParser() {
    }

    public static String humanMessage(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return "";
        }
        String trimmed = rawBody.trim();
        if (trimmed.startsWith("PUT ")) {
            int arrow = trimmed.indexOf(" → ");
            if (arrow >= 0) {
                trimmed = trimmed.substring(arrow + 3).trim();
            }
        }
        try {
            JsonNode node = MAPPER.readTree(trimmed);
            if (node.isArray() && node.size() >= 2) {
                String code = node.get(0).asText("");
                String detail = node.get(1).isNull() ? "" : node.get(1).asText("");
                if (!detail.isBlank()) {
                    return code + ": " + detail;
                }
                return code + profileRightsHint(code);
            }
        } catch (Exception ignored) {
            // corpo não é JSON
        }
        return trimmed;
    }

    private static String profileRightsHint(String code) {
        if ("ERROR_GLPI_ADD".equals(code)) {
            return " (habilite «Criar» na aba Perfis da definição do ativo; ver files/_logs no GLPI)";
        }
        if (!"ERROR_GLPI_UPDATE".equals(code)) {
            return "";
        }
        return " (habilite «Atualizar todos» na aba Perfis da definição do ativo; ver files/_logs no GLPI)";
    }
}
