package com.devgomes.glpi_integration.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ComputerUpdateRequestTest {

    @Test
    void toInputMap_includesOnlyNonNullFields() {
        var request = new ComputerUpdateRequest(42, null, null, 1, 11, null, null, null, "SN-1", null, null);

        var map = request.toInputMap();

        assertThat(map).containsEntry("users_id", 42);
        assertThat(map).containsEntry("states_id", 1);
        assertThat(map).containsEntry("computermodels_id", 11);
        assertThat(map).containsEntry("serial", "SN-1");
        assertThat(map).doesNotContainKey("groups_id");
    }
}
