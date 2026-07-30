package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.infrastructure.persistence.adapter.ImportSessionRepositoryImpl;

@DataJpaTest
@ActiveProfiles("test")
@Import({
    ImportSessionRepositoryImpl.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ImportSessionRepositoryTests extends ContainerTestConfig {

    @Autowired
    private ImportSessionRepository importSessionRepository;

    @Test
    void whenSaveCancelledSession_thenPersistsStatus() {
        var session = session(ImportSessionStatus.CANCELLED);

        var saved = importSessionRepository.save(session);
        var found = importSessionRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(ImportSessionStatus.CANCELLED);
    }

    private static ImportSession session(ImportSessionStatus status) {
        var now = Instant.now();
        var userId = UUID.randomUUID();
        return new ImportSession(
            UUID.randomUUID(),
            ImportType.SCHOOL_CLASS,
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
