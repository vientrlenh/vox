package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.repository.ImportRowRepository;
import com.sep.vox.infrastructure.persistence.adapter.ImportRowRepositoryImpl;

@DataJpaTest
@ActiveProfiles("test")
@Import({
    ImportRowRepositoryImpl.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ImportRowRepositoryTests extends ContainerTestConfig {

    @Autowired
    private ImportRowRepository importRowRepository;

    @Test
    void whenFindBySessionIdWithStatus_thenReturnsMatchingRowsSortedByRowNumber() {
        var sessionId = UUID.randomUUID();
        var otherSessionId = UUID.randomUUID();
        importRowRepository.saveAll(List.of(
            row(sessionId, 3L, ImportRowStatus.INVALID),
            row(sessionId, 1L, ImportRowStatus.INVALID),
            row(sessionId, 2L, ImportRowStatus.IMPORTED),
            row(otherSessionId, 1L, ImportRowStatus.INVALID)
        ));

        var found = importRowRepository.findBySessionId(sessionId, ImportRowStatus.INVALID, 1, 10);

        assertThat(found.content()).hasSize(2);
        assertThat(found.content()).extracting(row -> row.getRowNumber()).containsExactly(1L, 3L);
        assertThat(found.content()).extracting(row -> row.getStatus()).containsOnly(ImportRowStatus.INVALID);
        assertThat(found.totalElements()).isEqualTo(2L);
        assertThat(found.totalPages()).isEqualTo(1);
    }

    @Test
    void whenFindBySessionIdWithoutStatus_thenReturnsPagedRows() {
        var sessionId = UUID.randomUUID();
        importRowRepository.saveAll(List.of(
            row(sessionId, 1L, ImportRowStatus.PENDING),
            row(sessionId, 2L, ImportRowStatus.INVALID),
            row(sessionId, 3L, ImportRowStatus.IMPORTED)
        ));

        var found = importRowRepository.findBySessionId(sessionId, null, 2, 2);

        assertThat(found.content()).hasSize(1);
        assertThat(found.content().get(0).getRowNumber()).isEqualTo(3L);
        assertThat(found.page()).isEqualTo(2);
        assertThat(found.size()).isEqualTo(2);
        assertThat(found.totalElements()).isEqualTo(3L);
        assertThat(found.totalPages()).isEqualTo(2);
    }

    private static ImportRow row(UUID sessionId, long rowNumber, ImportRowStatus status) {
        return new ImportRow(
            sessionId,
            rowNumber,
            "{\"code\":\"A01\"}",
            null,
            null,
            status
        );
    }
}
