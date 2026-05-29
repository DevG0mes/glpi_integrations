package com.devgomes.glpi_integration.sync;

/**
 * Indica se a linha da planilha deve criar um item novo ou atualizar um existente.
 */
public record ResolvedAssetTarget(boolean create, int itemId, String matchedBy) {

    public static ResolvedAssetTarget update(int itemId, String matchedBy) {
        return new ResolvedAssetTarget(false, itemId, matchedBy);
    }

    public static ResolvedAssetTarget createNew() {
        return new ResolvedAssetTarget(true, 0, "novo (nome não existe no GLPI)");
    }
}
