package com.devgomes.glpi_integration.sync;



import com.devgomes.glpi_integration.config.GlpiCustomAssetsProperties;



import java.util.Map;



final class CustomAssetTargetResolver {



    private CustomAssetTargetResolver() {

    }



    static ResolvedAssetTarget resolve(

            CustomAssetRow row,

            GlpiCustomAssetsProperties.CustomAssetDefinition definition,

            Map<String, Integer> naturalKeyIndex) {

        if (row.glpiId() > 0) {

            return ResolvedAssetTarget.update(row.glpiId(), "id_ativo=" + row.glpiId());

        }

        String key = naturalKeyValue(row, definition);

        if (key == null || key.isBlank()) {

            throw new IllegalArgumentException(

                    CustomAssetNaturalKeySupport.rowMissingKeyMessage(row.lineNumber(), definition));

        }

        Integer id = naturalKeyIndex.get(SyncFieldResolver.normalizeLabelKey(key));

        if (id != null) {

            String alias = primaryAlias(definition);

            return ResolvedAssetTarget.update(id, alias + "='" + key + "'");

        }

        return ResolvedAssetTarget.createNew();

    }



    static String naturalKeyValue(

            CustomAssetRow row,

            GlpiCustomAssetsProperties.CustomAssetDefinition definition) {

        return CustomAssetNaturalKeySupport.valueFromRow(row, definition);

    }



    private static String primaryAlias(GlpiCustomAssetsProperties.CustomAssetDefinition definition) {

        var aliases = CustomAssetNaturalKeySupport.spreadsheetAliases(definition);

        return aliases.isEmpty() ? definition.naturalKeyField() : aliases.getFirst();

    }

}


