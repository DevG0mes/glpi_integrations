package com.devgomes.glpi_integration.sync;

/**
 * Linha da planilha para atualização de Computer no GLPI.
 * Identificação: {@code glpi_id} / {@code id_ativo} numérico OU {@code id_ativo} textual (name no GLPI).
 */
public record AssetUpdateRow(
        int lineNumber,
        int glpiId,
        String assetName,
        Integer usersId,
        String responsibleLogin,
        Integer statesId,
        String statusLabel,
        Integer computermodelsId,
        String serial,
        String otherserial,
        String comment,
        Integer groupsId,
        String groupLabel,
        Integer locationsId,
        String locationLabel,
        Integer computertypesId,
        String computerTypeLabel,
        Integer manufacturersId,
        String manufacturerLabel,
        String displayName
) {

    public boolean usesNameLookup() {
        return glpiId <= 0;
    }

    public boolean hasModelUpdate() {
        return computermodelsId != null && computermodelsId > 0;
    }
}
