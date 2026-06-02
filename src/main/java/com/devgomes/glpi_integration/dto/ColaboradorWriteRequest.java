package com.devgomes.glpi_integration.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Body JSON para criar/atualizar Colaborador (campos da planilha / tela GLPI).
 */
public record ColaboradorWriteRequest(
        String nome,
        String departamento,
        String email,
        String ativo
) {
    public Map<String, String> toColumnMap() {
        Map<String, String> map = new LinkedHashMap<>();
        put(map, "nome", nome);
        put(map, "departamento", departamento);
        put(map, "email", email);
        put(map, "ativo", ativo);
        return map;
    }

    private static void put(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value.trim());
        }
    }
}
