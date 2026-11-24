package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.infrastructure.persistence.adapter.ImportSessionRepositoryImpl;
import com.sep.vox.infrastructure.security.CustomUserDetails;
import com.sep.vox.support.RepositoryTest;

import jakarta.persistence.EntityManager;

@RepositoryTest
@Import({
    ImportSessionRepositoryImpl.class
})
class ImportSessionRepositoryTests {

    @Autowired
    private ImportSessionRepository importSessionRepository;

    @Autowired
    private EntityManager entityManager;

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

    @Test
    void whenMarkedQueued_thenUpdatedByIsTheCurrentUserAndUpdatedAtMoves() {
        UUID userId = UUID.randomUUID();
        ImportSession saved = importSessionRepository.save(session(ImportSessionStatus.PREVIEWED));
        logIn(userId);

        int queued = importSessionRepository.markQueued(saved.getId(), ImportType.SCHOOL_CLASS.name(), "{}", Instant.now());

        ImportSession stored = reload(saved);
        assertThat(queued).isEqualTo(1);
        assertThat(stored.getStatus()).isEqualTo(ImportSessionStatus.QUEUED);
        assertThat(stored.getUpdatedBy()).isEqualTo(userId.toString());
        assertThat(stored.getUpdatedAt()).isAfterOrEqualTo(saved.getUpdatedAt().truncatedTo(ChronoUnit.MICROS));
    }

    @Test
    void whenClaimed_thenClaimedByHoldsTheWorkerAndUpdatedByIsTheAuditActor() {
        UUID worker = UUID.randomUUID();
        ImportSession saved = importSessionRepository.save(session(ImportSessionStatus.QUEUED));
        Instant now = Instant.now();

        int claimed = importSessionRepository.markClaimed(List.of(saved.getId()), worker, now, now.plusSeconds(60));

        ImportSession stored = reload(saved);
        assertThat(claimed).isEqualTo(1);
        assertThat(stored.getClaimedBy()).isEqualTo(worker);
        assertThat(stored.getUpdatedBy()).isEqualTo("System");
        assertThat(stored.getUpdatedAt()).isAfterOrEqualTo(saved.getUpdatedAt().truncatedTo(ChronoUnit.MICROS));
    }

    @Test
    void whenAnExpiredLeaseIsRequeued_thenUpdatedAtMoves() {
        ImportSession expired = session(ImportSessionStatus.IMPORTING);
        expired.setLeaseExpiresAt(Instant.now().minusSeconds(60));
        ImportSession saved = importSessionRepository.save(expired);

        int requeued = importSessionRepository.requeueExpiredLeases(Instant.now(), 3);

        ImportSession stored = reload(saved);
        assertThat(requeued).isEqualTo(1);
        assertThat(stored.getStatus()).isEqualTo(ImportSessionStatus.QUEUED);
        assertThat(stored.getUpdatedAt()).isAfterOrEqualTo(saved.getUpdatedAt().truncatedTo(ChronoUnit.MICROS));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static void logIn(UUID userId) {
        CustomUserDetails principal = CustomUserDetails.builder().id(userId).build();
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private ImportSession reload(ImportSession session) {
        entityManager.flush();
        entityManager.clear();
        return importSessionRepository.findById(session.getId()).orElseThrow();
    }

    private static ImportSession session(ImportSessionStatus status) {
        return session(status, ImportType.SCHOOL_CLASS);
    }

    private static ImportSession session(ImportSessionStatus status, ImportType type) {
        Instant now = Instant.now();
        UUID userId = UUID.randomUUID();
        return ImportSession.builder()
            .schoolId(UUID.randomUUID())
            .type(type)
            .fileName("classes.csv")
            .originalHeadersJson("[]")
            .suggestedMappingJson("{}")
            .confirmedMappingJson(null)
            .validRows(0L)
            .invalidRows(0L)
            .importedRows(0L)
            .skippedRows(0L)
            .totalRows(1L)
            .failureReason("User cancelled")
            .status(status)
            .importedEntityId(null)
            .expiresAt(now.plus(1, ChronoUnit.DAYS))
            .claimedAt(null)
            .claimedBy(null)
            .leaseExpiresAt(null)
            .attempts(0)
            .createdAt(now)
            .updatedAt(now)
            .createdBy(userId.toString())
            .updatedBy(userId.toString())
            .build();
    }
}
