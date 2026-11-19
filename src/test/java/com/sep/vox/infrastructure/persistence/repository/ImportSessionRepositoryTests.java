package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.infrastructure.persistence.adapter.ImportSessionRepositoryImpl;
import com.sep.vox.support.RepositoryTest;

@RepositoryTest
@Import({
    ImportSessionRepositoryImpl.class
})
class ImportSessionRepositoryTests {

    @Autowired
    private ImportSessionRepository importSessionRepository;

    @Test
    void whenSaveCancelledSession_thenPersistsStatus() {
        ImportSession session = session(ImportSessionStatus.CANCELLED);

        ImportSession saved = importSessionRepository.save(session);
        Optional<ImportSession> found = importSessionRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(ImportSessionStatus.CANCELLED);
    }

    @ParameterizedTest
    @EnumSource(ImportType.class)
    void should_persist_every_import_type(ImportType type) {
        ImportSession session = session(ImportSessionStatus.PREVIEWED, type);

        ImportSession saved = importSessionRepository.save(session);
        Optional<ImportSession> found = importSessionRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getType()).isEqualTo(type);
    }

    private static ImportSession session(ImportSessionStatus status) {
        return session(status, ImportType.SCHOOL_CLASS);
    }

    private static ImportSession session(ImportSessionStatus status, ImportType type) {
        Instant now = Instant.now();
        UUID userId = UUID.randomUUID();
        return new ImportSession(
            UUID.randomUUID(),
            type,
            "classes.csv",
            "[]",
            "{}",
            null,
            0L,
            0L,
            0L,
            0L,
            1L,
            "User cancelled",
            status,
            null,
            now.plus(1, ChronoUnit.DAYS), 
            null, 
            null, 
            null, 
            0, 
            now,
            now,
            userId,
            userId
        );
    }
}
