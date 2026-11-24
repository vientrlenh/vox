package com.sep.vox.domain.model.importfile;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ImportRowTests {

    @Test
    void pendingSetsTheGivenValuesAndLeavesTheRowPending() {
        UUID sessionId = UUID.randomUUID();

        ImportRow row = ImportRow.pending(sessionId, 3L, "{\"a\":1}");

        assertThat(row.getSessionId()).isEqualTo(sessionId);
        assertThat(row.getRowNumber()).isEqualTo(3L);
        assertThat(row.getRawDataJson()).isEqualTo("{\"a\":1}");
        assertThat(row.getStatus()).isEqualTo(ImportRowStatus.PENDING);
        assertThat(row.getId()).isNull();
        assertThat(row.getMappedDataJson()).isNull();
        assertThat(row.getErrorsJson()).isNull();
    }
}
