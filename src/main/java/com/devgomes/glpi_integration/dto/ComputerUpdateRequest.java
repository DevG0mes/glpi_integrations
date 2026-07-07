package com.devgomes.glpi_integration.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Campos de atualização de Computer via API legada (somente IDs numéricos onde aplicável).
 */
public record ComputerUpdateRequest(
        @JsonProperty("users_id") Integer usersId,
        @JsonProperty("groups_id") Integer groupsId,
        @JsonProperty("locations_id") Integer locationsId,
        @JsonProperty("states_id") Integer statesId,
        @JsonProperty("computermodels_id") Integer computermodelsId,
        @JsonProperty("computertypes_id") Integer computertypesId,
        @JsonProperty("manufacturers_id") Integer manufacturersId,
        String name,
        String serial,
        String otherserial,
        String comment,
        @JsonProperty("vencimento_garantia") String vencimentoGarantia,
        @JsonProperty("cod_mega") String codMega
) {

    public Map<String, Object> toInputMap() {
        Map<String, Object> input = new LinkedHashMap<>();
        putIfNotNull(input, "users_id", usersId);
        putIfNotNull(input, "groups_id", groupsId);
        putIfNotNull(input, "locations_id", locationsId);
        putIfNotNull(input, "states_id", statesId);
        putIfNotNull(input, "computermodels_id", computermodelsId);
        putIfNotNull(input, "computertypes_id", computertypesId);
        putIfNotNull(input, "manufacturers_id", manufacturersId);
        putIfNotNull(input, "name", name);
        putIfNotNull(input, "serial", serial);
        putIfNotNull(input, "otherserial", otherserial);
        putIfNotNull(input, "comment", comment);
        putIfNotNull(input, "vencimento_garantia", vencimentoGarantia);
        putIfNotNull(input, "cod_mega", codMega);
        return input;
    }

    private static void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
